package dev.romoslayer.seasonfall.time;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.romoslayer.seasonfall.api.Season;
import dev.romoslayer.seasonfall.config.SeasonfallConfig;
import org.junit.jupiter.api.Test;

class YearCalendarTest {
	private static final long DAY = YearCalendar.TICKS_PER_DAY;

	private static SeasonfallConfig.SeasonLengths lengths(int spring, int summer, int autumn, int winter) {
		SeasonfallConfig.SeasonLengths lengths = new SeasonfallConfig.SeasonLengths();
		lengths.springLengthDays = spring;
		lengths.summerLengthDays = summer;
		lengths.autumnLengthDays = autumn;
		lengths.winterLengthDays = winter;
		return lengths;
	}

	@Test
	void everySeasonIsAQuarterOfTheYearWhateverItsLength() {
		SeasonfallConfig.SeasonLengths lengths = lengths(10, 30, 20, 40);
		assertEquals(0.0F, new YearCalendar(0, lengths).yearProgress(), 1e-6);
		assertEquals(0.25F, new YearCalendar(10 * DAY, lengths).yearProgress(), 1e-6);
		assertEquals(0.5F, new YearCalendar(40 * DAY, lengths).yearProgress(), 1e-6);
		assertEquals(0.75F, new YearCalendar(60 * DAY, lengths).yearProgress(), 1e-6);
		assertEquals(0.625F, new YearCalendar(50 * DAY, lengths).yearProgress(), 1e-6);
		assertEquals(Season.WINTER, new YearCalendar(99 * DAY, lengths).season());
	}

	@Test
	void yearWrapsAround() {
		SeasonfallConfig.SeasonLengths lengths = lengths(24, 24, 24, 24);
		YearCalendar nextYear = new YearCalendar(96 * DAY + 5 * DAY, lengths);
		assertEquals(2, nextYear.year());
		assertEquals(6, nextYear.dayOfYear());
		assertEquals(Season.SPRING, nextYear.season());
		// Before the first year (looking back past the start) wraps too
		assertEquals(Season.WINTER, new YearCalendar(-DAY, lengths).season());
	}

	@Test
	void jumpsLandInTheCurrentYear() {
		SeasonfallConfig.SeasonLengths lengths = lengths(24, 24, 24, 24);
		YearCalendar calendar = new YearCalendar(3 * 96 * DAY + 10 * DAY, lengths);
		long autumn = calendar.at(Season.AUTUMN, 0.5F);
		YearCalendar moved = new YearCalendar(autumn, lengths);
		assertEquals(Season.AUTUMN, moved.season());
		assertEquals(4, moved.year());
		assertEquals(13, moved.dayOfSeason());
	}

	@Test
	void longestAllowedSeasonsStayPositive() {
		int max = SeasonfallConfig.MAX_SEASON_DAYS;
		YearCalendar calendar = new YearCalendar(Long.MAX_VALUE / 2, lengths(max, max, max, max));
		assertEquals(4 * max, calendar.yearLengthDays());
		assertTrue(calendar.dayOfYear() >= 1 && calendar.dayOfYear() <= 4 * max);
		assertTrue(calendar.year() >= 1);
		float progress = calendar.yearProgress();
		assertTrue(progress >= 0.0F && progress < 1.0F);
	}

	@Test
	void savingNeverWrapsTheCounter() {
		assertEquals(Long.MAX_VALUE, SeasonClock.saturatedAdd(Long.MAX_VALUE - 5, 100));
		assertEquals(105, SeasonClock.saturatedAdd(5, 100));
	}
}
