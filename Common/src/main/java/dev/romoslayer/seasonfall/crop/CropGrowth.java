package dev.romoslayer.seasonfall.crop;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import dev.romoslayer.seasonfall.Seasonfall;
import dev.romoslayer.seasonfall.climate.BiomeProfile;
import dev.romoslayer.seasonfall.climate.Climate;
import dev.romoslayer.seasonfall.config.SeasonValues;
import dev.romoslayer.seasonfall.config.SeasonfallConfig;
import dev.romoslayer.seasonfall.time.SeasonClock;
import dev.romoslayer.seasonfall.time.YearCalendar;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.Nullable;

/**
 * Seasonal crop and plant growth, as a speed multiplier on the random ticks plants grow on (see {@link GrowthTicks}).
 * How plants grow is left entirely to the game and to other mods.
 * <p>
 * The whole random tick of a block in one of the crop tags is scaled, not just its growth, so only plants whose random
 * tick is about growing belong in them: the vanilla ones listed by default all are.
 */
public final class CropGrowth {
	public static final TagKey<Block> WARM_SEASON = tag("crops/warm_season");
	public static final TagKey<Block> COOL_SEASON = tag("crops/cool_season");
	public static final TagKey<Block> DEFAULT = tag("crops/default");
	public static final TagKey<Block> VEGETATION = tag("vegetation");
	public static final TagKey<Block> GREENHOUSE_GLASS = tag("greenhouse_glass");
	private static final Gson GSON = new Gson();

	/** Each block's growth curve, worked out the first time it random-ticks. Empty for blocks that are not plants. */
	private static final Map<Block, Optional<SeasonValues>> CURVES = new IdentityHashMap<>();
	private static final Map<Block, SeasonValues> OVERRIDES = new IdentityHashMap<>();
	/**
	 * Set while an extra growth tick runs. Any random tick started from inside it (a block ticking a neighbour, say) runs
	 * as the game would, without seasonal scaling, so nothing is scaled twice.
	 */
	private static boolean extraTickRunning;

	private CropGrowth() {
	}

	/** Re-reads the per-crop overrides and forgets cached groups, after a config or data pack reload. */
	public static void rebuild(MinecraftServer server) {
		CURVES.clear();
		OVERRIDES.clear();
		SeasonfallConfig.Crops config = SeasonfallConfig.get().crops;
		for (Map.Entry<String, JsonElement> entry : config.cropOverrides.entrySet()) {
			ResourceLocation id = ResourceLocation.tryParse(entry.getKey());
			Optional<Block> block = id == null ? Optional.empty() : BuiltInRegistries.BLOCK.getOptional(id);
			if (block.isEmpty()) {
				// Probably a mod that is not installed
				continue;
			}
			// Already checked by the config: a known group name, or four usable speeds
			JsonElement value = entry.getValue();
			SeasonValues values = value.isJsonPrimitive() ? config.groups.get(value.getAsString()) : GSON.fromJson(value, SeasonValues.class);
			if (values != null) {
				OVERRIDES.put(block.get(), values);
			}
		}
	}

	/** Called in place of every random tick. */
	public static void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, Runnable tick) {
		if (extraTickRunning) {
			tick.run();
			return;
		}
		float multiplier = multiplier(state, level, pos);
		if (multiplier == 1.0F) {
			tick.run();
			return;
		}
		if (!GrowthTicks.runs(multiplier, random.nextFloat())) {
			return;
		}
		tick.run();
		int extra = GrowthTicks.extra(multiplier, random.nextFloat());
		for (int i = 0; i < extra; i++) {
			// Always the block as it is now: stop once it has changed (grown up, been eaten, replaced)
			BlockState now = level.getBlockState(pos);
			if (now.getBlock() != state.getBlock() || !now.isRandomlyTicking()) {
				return;
			}
			extraTickRunning = true;
			try {
				now.randomTick(level, pos, random);
			} finally {
				extraTickRunning = false;
			}
		}
	}

	/** How fast this plant grows here right now (1 = normal). */
	public static float multiplier(BlockState state, ServerLevel level, BlockPos pos) {
		Growth growth = growth(state, level, pos);
		if (growth == null) {
			return 1.0F;
		}
		float speed = growth.at(Seasonfall.clock().yearProgress());
		return GrowthTicks.effective(speed < 1.0F && isUnderGlass(level, pos) ? 1.0F : speed);
	}

	/**
	 * How fast this plant grew here on average over the {@code elapsedTicks} game ticks leading up to now (1 = normal),
	 * following the year back through what it actually did: seasons passing, pauses and commands that moved it. For mods
	 * that grow plants in chunks nobody was near, such as Elapsed. Before Seasonfall kept a record, the year is assumed
	 * to have run at its normal pace; the current season lengths and settings are used throughout.
	 */
	public static float averageMultiplier(BlockState state, ServerLevel level, BlockPos pos, long elapsedTicks) {
		Growth growth = growth(state, level, pos);
		if (growth == null) {
			return 1.0F;
		}
		SeasonClock clock = Seasonfall.clock();
		if (elapsedTicks <= 0) {
			return multiplier(state, level, pos);
		}
		SeasonfallConfig.SeasonLengths lengths = SeasonfallConfig.get().seasonLength;
		long yearTicks = new YearCalendar(0, lengths).yearTicks();
		boolean[] underGlass = new boolean[2];
		double average = GrowthAverage.over(clock.gameTime(), elapsedTicks, yearTicks, gameTime -> {
			float speed = growth.at(new YearCalendar(clock.seasonTicksAt(gameTime), lengths).yearProgress());
			if (speed < 1.0F) {
				// [checked, under glass]: the roof is only looked for once, and only if it matters
				if (!underGlass[0]) {
					underGlass[0] = true;
					underGlass[1] = isUnderGlass(level, pos);
				}
				if (underGlass[1]) {
					speed = 1.0F;
				}
			}
			return GrowthTicks.effective(speed);
		});
		return (float) average;
	}

	/** A plant's speed through the year at its spot, or null when the season does not change it here. */
	private static @Nullable Growth growth(BlockState state, ServerLevel level, BlockPos pos) {
		SeasonfallConfig config = SeasonfallConfig.get();
		if (Seasonfall.clock() == null || !config.general.enabled || !config.crops.cropSeasonEffects || !Climate.hasSeasons(level)) {
			return null;
		}
		Optional<SeasonValues> curve = CURVES.computeIfAbsent(state.getBlock(), block -> Optional.ofNullable(curveFor(state)));
		if (curve.isEmpty()) {
			return null;
		}
		BiomeProfile profile = Climate.profile(level.getBiome(pos));
		if (profile == null) {
			// A biome without seasons
			return null;
		}
		// A biome's own fixed speeds replace the crop groups, but are still scaled by its overall season strength
		SeasonValues fixed = profile.cropMultipliers();
		return fixed != null ? new Growth(fixed, profile.seasonStrength()) : new Growth(curve.get(), profile.cropSeasonality());
	}

	private record Growth(SeasonValues curve, float strength) {
		float at(float yearProgress) {
			return GrowthTicks.scaled(this.curve.at(yearProgress), this.strength);
		}
	}

	private static @Nullable SeasonValues curveFor(BlockState state) {
		SeasonValues override = OVERRIDES.get(state.getBlock());
		if (override != null) {
			return override;
		}
		Map<String, SeasonValues> groups = SeasonfallConfig.get().crops.groups;
		if (state.is(WARM_SEASON)) {
			return groups.get("warm_season");
		}
		if (state.is(COOL_SEASON)) {
			return groups.get("cool_season");
		}
		if (state.is(DEFAULT)) {
			return groups.get("default");
		}
		if (state.is(VEGETATION)) {
			return groups.get("vegetation");
		}
		return null;
	}

	/** Under glass, with nothing else solid in between, within the configured height. */
	private static boolean isUnderGlass(ServerLevel level, BlockPos pos) {
		SeasonfallConfig.Crops config = SeasonfallConfig.get().crops;
		if (!config.greenhouses) {
			return false;
		}
		int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
		int top = Math.min(surface, pos.getY() + config.greenhouseMaxHeight + 1);
		BlockPos.MutableBlockPos cursor = pos.mutable();
		for (int y = pos.getY() + 1; y < top; y++) {
			BlockState above = level.getBlockState(cursor.setY(y));
			if (above.is(GREENHOUSE_GLASS)) {
				return true;
			}
			if (!above.getCollisionShape(level, cursor).isEmpty()) {
				return false;
			}
		}
		return false;
	}

	private static TagKey<Block> tag(String path) {
		return TagKey.create(Registries.BLOCK, Seasonfall.id(path));
	}
}
