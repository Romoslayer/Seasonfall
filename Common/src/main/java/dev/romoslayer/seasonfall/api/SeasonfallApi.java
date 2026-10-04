package dev.romoslayer.seasonfall.api;

import dev.romoslayer.seasonfall.Seasonfall;
import dev.romoslayer.seasonfall.climate.Climate;
import dev.romoslayer.seasonfall.config.SeasonfallConfig;
import dev.romoslayer.seasonfall.crop.CropGrowth;
import dev.romoslayer.seasonfall.time.SeasonClock;
import dev.romoslayer.seasonfall.time.YearCalendar;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What other mods (Stormcell in particular) can ask Seasonfall. Everything here is safe to call from the server thread
 * at any time; while no world is running, or Seasonfall is switched off, it reports a neutral climate.
 * <p>
 * Seasonfall decides the long-term climate, a weather mod decides the weather: a weather mod should read
 * {@link #climate} where it simulates weather, and Seasonfall never creates weather itself.
 */
public final class SeasonfallApi {
	private SeasonfallApi() {
	}

	/** Whether a world is running with Seasonfall switched on. */
	public static boolean isActive() {
		return Seasonfall.clock() != null && SeasonfallConfig.get().general.enabled;
	}

	/** How far through the year it is: 0 = start of spring, 0.25 summer, 0.5 autumn, 0.75 winter, wrapping at 1. */
	public static float yearProgress() {
		SeasonClock clock = Seasonfall.clock();
		return clock == null ? 0.0F : clock.yearProgress();
	}

	public static Season season() {
		return Season.ofPhase(yearProgress());
	}

	/** How far through the current season, 0 to 1. */
	public static float seasonProgress() {
		SeasonClock clock = Seasonfall.clock();
		return clock == null ? 0.0F : clock.calendar().seasonProgress();
	}

	/** 1-based year number. */
	public static int year() {
		SeasonClock clock = Seasonfall.clock();
		return clock == null ? 1 : clock.calendar().year();
	}

	/** Length of the year in in-game days. */
	public static int yearLengthDays() {
		SeasonClock clock = Seasonfall.clock();
		return clock == null ? 0 : (int) (clock.calendar().yearTicks() / YearCalendar.TICKS_PER_DAY);
	}

	/** Whether this dimension has seasons at all. */
	public static boolean hasSeasons(ServerLevel level) {
		return isActive() && SeasonfallConfig.get().dimensions.hasSeasons(level.dimension().identifier().toString());
	}

	/** The seasonal climate at a place: what a weather mod should apply on top of the biome's normal weather. */
	public static ClimateModifiers climate(ServerLevel level, BlockPos pos) {
		return hasSeasons(level) ? climate(level.getBiome(pos)) : ClimateModifiers.NEUTRAL;
	}

	/** How fast this plant grows here right now (1 = normal, 0 = not at all), greenhouses included. */
	public static float cropGrowthMultiplier(ServerLevel level, BlockPos pos, BlockState state) {
		return CropGrowth.multiplier(state, level, pos);
	}

	/**
	 * How fast this plant grew here on average over the {@code elapsedTicks} game ticks leading up to now, following the
	 * year back through the seasons it passed (1 = normal, 0 = not at all). For mods that grow plants in chunks nobody
	 * was near, such as Elapsed: a crop left alone through autumn and winter grows by those seasons, not by the one it is
	 * when someone comes back. Cheap, and reads nothing outside the plant's own column.
	 */
	public static float averageCropGrowthMultiplier(ServerLevel level, BlockPos pos, BlockState state, long elapsedTicks) {
		return CropGrowth.averageMultiplier(state, level, pos, elapsedTicks);
	}

	/** The seasonal climate of a biome, wherever it is. */
	public static ClimateModifiers climate(Holder<Biome> biome) {
		SeasonfallConfig config = SeasonfallConfig.get();
		if (!isActive() || !config.stormcell.stormcellIntegration) {
			return ClimateModifiers.NEUTRAL;
		}
		return Climate.modifiers(biome.value(), yearProgress());
	}
}
