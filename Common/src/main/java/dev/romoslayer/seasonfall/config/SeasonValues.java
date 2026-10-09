package dev.romoslayer.seasonfall.config;

import dev.romoslayer.seasonfall.climate.Keyframes;
import java.util.Objects;

/**
 * One value for the middle of each season; anywhere else in the year it is blended smoothly between them. Seasons left
 * out of the config file read as missing ({@code NaN}) until {@link #completed} fills them in.
 */
public final class SeasonValues {
	public float spring = Float.NaN;
	public float summer = Float.NaN;
	public float autumn = Float.NaN;
	public float winter = Float.NaN;

	/** For Gson: every season missing until the file says otherwise. */
	public SeasonValues() {
	}

	public SeasonValues(float spring, float summer, float autumn, float winter) {
		this.spring = spring;
		this.summer = summer;
		this.autumn = autumn;
		this.winter = winter;
	}

	public static SeasonValues neutral() {
		return new SeasonValues(1.0F, 1.0F, 1.0F, 1.0F);
	}

	/** The value at a point in the year (0 = start of spring, 1 = start of the next spring). Allocates nothing. */
	public float at(float yearProgress) {
		return Keyframes.cyclic(this.spring, this.summer, this.autumn, this.winter, 0.5F, yearProgress);
	}

	/**
	 * A copy where every season is a finite number within [min, max]. Missing or broken seasons take the matching
	 * season of {@code fallback}; anything outside the range is clamped. Each change is reported to {@code problems}.
	 */
	public SeasonValues completed(SeasonValues fallback, float min, float max, String path, Problems problems) {
		return new SeasonValues(
				fix(this.spring, fallback.spring, min, max, path + ".spring", problems),
				fix(this.summer, fallback.summer, min, max, path + ".summer", problems),
				fix(this.autumn, fallback.autumn, min, max, path + ".autumn", problems),
				fix(this.winter, fallback.winter, min, max, path + ".winter", problems));
	}

	private static float fix(float value, float fallback, float min, float max, String path, Problems problems) {
		if (Float.isNaN(value)) {
			// Simply left out: not worth a warning
			return fallback;
		}
		if (!Float.isFinite(value)) {
			problems.add(path + " is not a usable number; using " + fallback);
			return fallback;
		}
		if (value < min || value > max) {
			float clamped = Math.max(min, Math.min(max, value));
			problems.add(path + " must be between " + min + " and " + max + "; using " + clamped);
			return clamped;
		}
		return value;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof SeasonValues values && Float.compare(this.spring, values.spring) == 0 && Float.compare(this.summer, values.summer) == 0
				&& Float.compare(this.autumn, values.autumn) == 0 && Float.compare(this.winter, values.winter) == 0;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.spring, this.summer, this.autumn, this.winter);
	}

	@Override
	public String toString() {
		return "{spring " + this.spring + ", summer " + this.summer + ", autumn " + this.autumn + ", winter " + this.winter + "}";
	}
}
