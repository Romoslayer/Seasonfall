package dev.romoslayer.seasonfall.crop;

import dev.romoslayer.seasonfall.config.SeasonfallConfig;

/**
 * Turns a growth speed into random ticks. On average a plant gets exactly {@code multiplier} ticks for every one the
 * game gives it: below 1 some are skipped, above 1 some are followed by one or more extra ones (up to
 * {@link SeasonfallConfig#MAX_GROWTH} in all).
 */
public final class GrowthTicks {
	private GrowthTicks() {
	}

	/** The speed actually applied: never below 0 nor above {@link SeasonfallConfig#MAX_GROWTH}. */
	public static float effective(float multiplier) {
		return Float.isFinite(multiplier) ? Math.clamp(multiplier, 0.0F, SeasonfallConfig.MAX_GROWTH) : 1.0F;
	}

	/** Whether the game's own random tick happens. {@code roll} is uniform in [0, 1). */
	public static boolean runs(float multiplier, float roll) {
		float effective = effective(multiplier);
		return effective >= 1.0F || roll < effective;
	}

	/** How many extra random ticks follow the game's own. {@code roll} is uniform in [0, 1). */
	public static int extra(float multiplier, float roll) {
		float over = effective(multiplier) - 1.0F;
		if (over <= 0.0F) {
			return 0;
		}
		int whole = (int) over;
		return whole + (roll < over - whole ? 1 : 0);
	}

	/** A biome's speed from a crop group's: the group's change from normal, scaled by how seasonal the biome is. */
	public static float scaled(float groupSpeed, float seasonality) {
		return 1.0F + (groupSpeed - 1.0F) * seasonality;
	}
}
