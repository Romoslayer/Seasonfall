package dev.romoslayer.seasonfall.crop;

import java.util.function.LongToDoubleFunction;

/** Averages a plant's growth speed over a stretch of game time that has passed. */
public final class GrowthAverage {
	/** How finely a whole year is sampled. */
	static final int SAMPLES_PER_YEAR = 96;
	/** Fewest samples, however short the absence: pauses and jumps can happen within a single day. */
	static final int MIN_SAMPLES = 32;
	/** Most samples for any one average, however long the absence. */
	static final int MAX_SAMPLES = 192;

	private GrowthAverage() {
	}

	/**
	 * The mean of {@code speedAt} over the {@code elapsedTicks} game ticks ending at {@code endGameTime}, sampled at the
	 * middle of evenly spaced slices: about {@link #SAMPLES_PER_YEAR} per year, and never fewer than {@link #MIN_SAMPLES}.
	 *
	 * @param yearTicks length of the year in season time, to choose how many samples to take
	 * @param speedAt   the speed at a game time (already following the year's history and any greenhouse)
	 */
	public static double over(long endGameTime, long elapsedTicks, long yearTicks, LongToDoubleFunction speedAt) {
		if (elapsedTicks <= 0) {
			return speedAt.applyAsDouble(endGameTime);
		}
		int samples = (int) Math.clamp((long) Math.ceil(elapsedTicks * (double) SAMPLES_PER_YEAR / Math.max(1, yearTicks)), MIN_SAMPLES, MAX_SAMPLES);
		double sum = 0.0;
		for (int i = 0; i < samples; i++) {
			long gameTime = endGameTime - elapsedTicks + (long) ((i + 0.5) * elapsedTicks / samples);
			sum += speedAt.applyAsDouble(gameTime);
		}
		return sum / samples;
	}
}
