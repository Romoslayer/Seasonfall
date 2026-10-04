package dev.romoslayer.seasonfall.time;

import dev.romoslayer.seasonfall.Seasonfall;
import dev.romoslayer.seasonfall.climate.Keyframes;
import dev.romoslayer.seasonfall.config.SeasonfallConfig;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.level.Level;

/**
 * Longer summer days and longer winter nights. The overworld clock runs slower while the sun is up and faster while it
 * is down (or the other way round), so a whole day still takes the usual 24000 ticks and everything that keeps time by
 * the day cycle carries on as normal. The clock's speed is part of the time the server sends players, so unmodified
 * clients move the sun and moon to match.
 */
public final class DayLength {
	private static final long DAY = YearCalendar.TICKS_PER_DAY;
	private static final long SUNSET = DAY / 2;

	private float appliedRate = Float.NaN;

	/** How long daytime is compared to normal at this point in the year. */
	public static float daytimeMultiplier(float yearProgress) {
		SeasonfallConfig.DayLength config = SeasonfallConfig.get().dayLength;
		float[] middles = {1.0F, config.summerDayLengthMultiplier, 1.0F, config.winterDayLengthMultiplier};
		// Equal days and nights at the equinoxes, in the middle of spring and autumn
		return Keyframes.cyclic(middles, 0.5F, yearProgress);
	}

	public void tick(MinecraftServer server, float yearProgress) {
		SeasonfallConfig config = SeasonfallConfig.get();
		if (!config.general.enabled || !config.dayLength.seasonalDayLength) {
			this.reset(server);
			return;
		}
		ServerLevel overworld = server.getLevel(Level.OVERWORLD);
		Optional<Holder<WorldClock>> clock = overworld == null ? Optional.empty() : overworld.dimensionType().defaultClock();
		if (clock.isEmpty()) {
			return;
		}
		long timeOfDay = Math.floorMod(overworld.getDefaultClockTime(), DAY);
		float daytime = daytimeMultiplier(yearProgress);
		// Daytime stretched by `daytime` and night squeezed to fit what is left of the 24000 ticks
		float rate = timeOfDay < SUNSET ? 1.0F / daytime : 1.0F / (2.0F - daytime);
		this.apply(server, clock.get(), Math.round(rate * 1000.0F) / 1000.0F);
	}

	/** Puts the clock back to normal speed, when the feature is switched off or the server stops. */
	public void reset(MinecraftServer server) {
		if (Float.isNaN(this.appliedRate)) {
			return;
		}
		ServerLevel overworld = server.getLevel(Level.OVERWORLD);
		if (overworld != null) {
			overworld.dimensionType().defaultClock().ifPresent(clock -> server.clockManager().setRate(clock, 1.0F));
		}
		this.appliedRate = Float.NaN;
	}

	private void apply(MinecraftServer server, Holder<WorldClock> clock, float rate) {
		if (rate == this.appliedRate) {
			return;
		}
		// Setting the rate sends every player the new time, so only do it when it actually changes
		server.clockManager().setRate(clock, rate);
		if (Float.isNaN(this.appliedRate)) {
			Seasonfall.LOGGER.debug("Seasonal day length took over the overworld clock (rate {})", rate);
		}
		this.appliedRate = rate;
	}
}
