package dev.romoslayer.seasonfall.time;

import dev.romoslayer.seasonfall.Seasonfall;
import dev.romoslayer.seasonfall.climate.Keyframes;
import dev.romoslayer.seasonfall.config.SeasonfallConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Longer summer days and longer nights in winter. The overworld's time of day runs slower while the sun is up and
 * faster while it is down (or the other way round), so a whole day still takes the usual 24000 ticks and everything that
 * keeps time by the day cycle carries on as normal. The server sends players the time of day every second, so
 * unmodified clients move the sun and moon to match.
 */
public final class DayLength {
	/**
	 * Daybreak gives every place its own time of day, so one overworld time speed would stretch the wrong part of the day
	 * far from spawn. With it installed the time of day is left alone and Daybreak applies {@link #daytimeFraction} per
	 * place.
	 */
	public static final String DAYBREAK = "daybreak";
	private static final long DAY = YearCalendar.TICKS_PER_DAY;
	private static final long SUNSET = DAY / 2;

	private float rate = 1.0F;
	/** The part of a tick the time of day is behind (or ahead of) where the rate puts it. */
	private double carry;
	private boolean active;

	/** How long daytime is compared to normal at this point in the year. */
	public static float daytimeMultiplier(float yearProgress) {
		SeasonfallConfig.DayLength config = SeasonfallConfig.get().dayLength;
		float[] middles = {1.0F, config.summerDayLengthMultiplier, 1.0F, config.winterDayLengthMultiplier};
		// Equal days and nights at the equinoxes, in the middle of spring and autumn
		return Keyframes.cyclic(middles, 0.5F, yearProgress);
	}

	/** The share of a 24000-tick day that is daytime at this point in the year (0.5 = equal day and night). */
	public static float daytimeFraction(float yearProgress) {
		return 0.5F * daytimeMultiplier(yearProgress);
	}

	public void tick(MinecraftServer server, float yearProgress) {
		SeasonfallConfig config = SeasonfallConfig.get();
		if (!config.general.enabled || !config.dayLength.seasonalDayLength || Seasonfall.platform().isModLoaded(DAYBREAK)) {
			this.reset();
			return;
		}
		ServerLevel overworld = server.getLevel(Level.OVERWORLD);
		if (overworld == null) {
			return;
		}
		long timeOfDay = Math.floorMod(overworld.getDayTime(), DAY);
		float daytime = daytimeMultiplier(yearProgress);
		// Daytime stretched by `daytime` and night squeezed to fit what is left of the 24000 ticks
		float rate = timeOfDay < SUNSET ? 1.0F / daytime : 1.0F / (2.0F - daytime);
		this.rate = Math.round(rate * 1000.0F) / 1000.0F;
		if (!this.active) {
			this.active = true;
			Seasonfall.LOGGER.debug("Seasonal day length took over the overworld's time of day (rate {})", this.rate);
		}
	}

	/** Puts the time of day back to its normal speed, when the feature is switched off or the server stops. */
	public void reset() {
		this.rate = 1.0F;
		this.carry = 0.0;
		this.active = false;
	}

	/**
	 * Where the overworld's time of day goes this tick, given where the game would move it: the game's own step (one tick,
	 * normally) at the seasonal rate, with whatever part of a tick is left over carried into the next.
	 */
	public long advance(long current, long proposed) {
		if (this.rate == 1.0F && this.carry == 0.0) {
			return proposed;
		}
		double exact = (proposed - current) * (double) this.rate + this.carry;
		long whole = (long) Math.floor(exact);
		this.carry = exact - whole;
		return current + whole;
	}
}
