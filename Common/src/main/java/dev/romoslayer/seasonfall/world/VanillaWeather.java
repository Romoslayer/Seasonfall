package dev.romoslayer.seasonfall.world;

import dev.romoslayer.seasonfall.Seasonfall;
import dev.romoslayer.seasonfall.config.SeasonfallConfig;
import dev.romoslayer.seasonfall.time.SeasonClock;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.valueproviders.IntProvider;

/**
 * Without a weather mod, nudges the game's own random weather to suit the time of year: showers come more or less often
 * and last longer or shorter, and thunderstorms are likelier in summer. Nothing more; weather simulation is Stormcell's
 * job, and this steps aside while Stormcell is installed.
 */
public final class VanillaWeather {
	public static final String STORMCELL = "stormcell";

	private VanillaWeather() {
	}

	/**
	 * @param provider which of the game's four weather timers was rolled
	 * @param ticks    what it rolled
	 */
	public static int adjust(IntProvider provider, int ticks) {
		SeasonClock clock = Seasonfall.clock();
		SeasonfallConfig config = SeasonfallConfig.get();
		if (clock == null || !config.general.enabled || !config.weather.vanillaWeatherAdjustments || Seasonfall.platform().isModLoaded(STORMCELL)) {
			return ticks;
		}
		float phase = clock.yearProgress();
		float factor;
		if (provider == ServerLevel.RAIN_DELAY) {
			// More precipitation: shorter dry spells
			factor = 1.0F / config.weather.precipitation.at(phase);
		} else if (provider == ServerLevel.RAIN_DURATION) {
			factor = config.weather.precipitation.at(phase);
		} else if (provider == ServerLevel.THUNDER_DURATION) {
			return ticks;
		} else {
			// The only other one is the (private) wait between thunderstorms
			factor = 1.0F / config.weather.storms.at(phase);
		}
		return Math.max(1, Math.round(ticks * factor));
	}
}
