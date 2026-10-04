package dev.romoslayer.seasonfall.crop;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.romoslayer.seasonfall.api.Season;
import dev.romoslayer.seasonfall.config.SeasonValues;
import dev.romoslayer.seasonfall.config.SeasonfallConfig;
import dev.romoslayer.seasonfall.time.SeasonHistory;
import dev.romoslayer.seasonfall.time.YearCalendar;
import org.junit.jupiter.api.Test;

/** The year's real history (pauses, jumps) decides the growth speed a plant had while nobody was near it. */
class GrowthAverageTest {
	private static final long DAY = YearCalendar.TICKS_PER_DAY;
	private static final SeasonValues DEFAULT_GROUP = new SeasonValues(1.15F, 1.1F, 0.85F, 0.4F);
	private static final SeasonfallConfig.SeasonLengths LENGTHS = new SeasonfallConfig.SeasonLengths();

	private static double average(SeasonHistory history, long end, long elapsed) {
		long yearTicks = new YearCalendar(0, LENGTHS).yearTicks();
		return GrowthAverage.over(end, elapsed, yearTicks,
				gameTime -> DEFAULT_GROUP.at(new YearCalendar(history.seasonAt(gameTime), LENGTHS).yearProgress()));
	}

	@Test
	void halfSummerThenAPausedWinter() {
		long midSummer = new YearCalendar(0, LENGTHS).at(Season.SUMMER, 0.5F);
		long midWinter = new YearCalendar(0, LENGTHS).at(Season.WINTER, 0.5F);
		SeasonHistory history = new SeasonHistory();
		long season = midSummer;
		// A day of summer...
		for (long tick = 0; tick < DAY / 2; tick++) {
			history.record(tick, season++);
		}
		// ...then an admin moves the year to midwinter and pauses it for the rest of the absence
		history.recordJump(DAY / 2, season, midWinter);
		history.recordJump(DAY / 2, midWinter, midWinter);
		for (long tick = DAY / 2; tick <= DAY; tick++) {
			history.record(tick, midWinter);
		}
		double average = average(history, DAY, DAY);
		// Half at the summer speed, half at the winter speed; not the winter speed for all of it
		assertEquals((1.1 + 0.4) / 2, average, 0.02);
	}

	@Test
	void unloadedThroughAWholeYearAveragesTheYear() {
		SeasonHistory history = new SeasonHistory();
		long year = new YearCalendar(0, LENGTHS).yearTicks();
		for (long tick = 0; tick <= year; tick += 20) {
			history.record(tick, tick);
		}
		double sum = 0;
		for (int i = 0; i < 10000; i++) {
			sum += DEFAULT_GROUP.at(i / 10000.0F);
		}
		assertEquals(sum / 10000, average(history, year, year), 0.01);
	}

	@Test
	void pausedTheWholeTimeIsThePausedSeason() {
		long midWinter = new YearCalendar(0, LENGTHS).at(Season.WINTER, 0.5F);
		SeasonHistory history = new SeasonHistory();
		history.recordJump(0, midWinter, midWinter);
		for (long tick = 0; tick <= 3 * DAY; tick++) {
			history.record(tick, midWinter);
		}
		assertEquals(0.4, average(history, 3 * DAY, 3 * DAY), 1e-4);
	}
}
