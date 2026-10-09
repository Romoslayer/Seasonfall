package dev.romoslayer.seasonfall.time;

import dev.romoslayer.seasonfall.api.Season;
import dev.romoslayer.seasonfall.config.SeasonfallConfig;

/**
 * Turns season time into dates. Seasons may have different lengths, but each always covers a quarter of the year's
 * progress (0 = start of spring, 0.25 = start of summer, and so on), so everything that follows the year looks the same
 * whatever the lengths.
 */
public final class YearCalendar {
	public static final long TICKS_PER_DAY = 24000L;

	private final long seasonTicks;
	private final long[] lengths = new long[4];
	private final long yearTicks;

	public YearCalendar(long seasonTicks, SeasonfallConfig.SeasonLengths config) {
		this.seasonTicks = seasonTicks;
		long year = 0;
		for (Season season : Season.values()) {
			this.lengths[season.ordinal()] = config.days(season) * TICKS_PER_DAY;
			year += this.lengths[season.ordinal()];
		}
		this.yearTicks = year;
	}

	public long yearTicks() {
		return this.yearTicks;
	}

	public int yearLengthDays() {
		return (int) (this.yearTicks / TICKS_PER_DAY);
	}

	public long tickOfYear() {
		return Math.floorMod(this.seasonTicks, this.yearTicks);
	}

	/** 1-based. */
	public int year() {
		return (int) Math.floorDiv(this.seasonTicks, this.yearTicks) + 1;
	}

	public Season season() {
		long tick = this.tickOfYear();
		for (Season season : Season.values()) {
			if (tick < this.lengths[season.ordinal()]) {
				return season;
			}
			tick -= this.lengths[season.ordinal()];
		}
		return Season.WINTER;
	}

	public long seasonStart(Season season) {
		long start = 0;
		for (int i = 0; i < season.ordinal(); i++) {
			start += this.lengths[i];
		}
		return start;
	}

	/** How far through the current season, 0 to 1. */
	public float seasonProgress() {
		Season season = this.season();
		return (float) (this.tickOfYear() - this.seasonStart(season)) / this.lengths[season.ordinal()];
	}

	/** How far through the year, 0 to 1, where each season is a quarter. */
	public float yearProgress() {
		return (this.season().ordinal() + this.seasonProgress()) * 0.25F;
	}

	/** 1-based day of the current season. */
	public int dayOfSeason() {
		return (int) ((this.tickOfYear() - this.seasonStart(this.season())) / TICKS_PER_DAY) + 1;
	}

	/** 1-based day of the year. */
	public int dayOfYear() {
		return (int) (this.tickOfYear() / TICKS_PER_DAY) + 1;
	}

	public int seasonLengthDays() {
		return (int) (this.lengths[this.season().ordinal()] / TICKS_PER_DAY);
	}

	/** "Early", "Mid" or "Late", by which third of the season it is. */
	public String part() {
		float progress = this.seasonProgress();
		return progress < 1.0F / 3.0F ? "Early" : progress < 2.0F / 3.0F ? "Mid" : "Late";
	}

	/** Season time at the start of this year plus the given tick of the year. */
	public long atTickOfYear(long tickOfYear) {
		return (this.seasonTicks - this.tickOfYear()) + Math.floorMod(tickOfYear, this.yearTicks);
	}

	/** Season time at a point some way into a season of the current year. */
	public long at(Season season, float progress) {
		long into = (long) (Math.max(0.0F, Math.min(0.9999F, progress)) * this.lengths[season.ordinal()]);
		return this.atTickOfYear(this.seasonStart(season) + into);
	}
}
