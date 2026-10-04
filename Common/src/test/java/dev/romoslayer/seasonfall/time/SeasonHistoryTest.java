package dev.romoslayer.seasonfall.time;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SeasonHistoryTest {
	private static final long DAY = 24000L;

	@Test
	void normalPlayIsOneLine() {
		SeasonHistory history = new SeasonHistory();
		for (long tick = 0; tick <= 10 * DAY; tick++) {
			history.record(tick, 5000 + tick);
		}
		assertEquals(5000 + 3 * DAY, history.seasonAt(3 * DAY));
		assertEquals(5000 + 10 * DAY, history.seasonAt(10 * DAY));
		assertTrue(history.size() <= 3, "a straight line needs only its ends, had " + history.size());
	}

	@Test
	void pauseIsFlatAndNotBlendedIntoARamp() {
		SeasonHistory history = new SeasonHistory();
		long season = 0;
		for (long tick = 0; tick < 5 * DAY; tick++) {
			history.record(tick, season++);
		}
		long pausedAt = season;
		history.recordJump(5 * DAY, pausedAt, pausedAt);
		for (long tick = 5 * DAY; tick < 25 * DAY; tick++) {
			history.record(tick, pausedAt);
		}
		history.recordJump(25 * DAY, pausedAt, pausedAt);
		for (long tick = 25 * DAY; tick <= 30 * DAY; tick++) {
			history.record(tick, season++);
		}
		// Throughout the pause the year stood still, exactly
		assertEquals(pausedAt, history.seasonAt(6 * DAY));
		assertEquals(pausedAt, history.seasonAt(15 * DAY));
		assertEquals(pausedAt, history.seasonAt(24 * DAY));
		assertEquals(2 * DAY, history.seasonAt(2 * DAY));
		assertEquals(pausedAt + 3 * DAY, history.seasonAt(28 * DAY), 1);
	}

	@Test
	void unannouncedPauseIsStillFoundWithinTolerance() {
		SeasonHistory history = new SeasonHistory();
		long season = 0;
		for (long tick = 0; tick < 5 * DAY; tick++) {
			history.record(tick, season++);
		}
		long stopped = season;
		for (long tick = 5 * DAY; tick < 25 * DAY; tick++) {
			history.record(tick, stopped);
		}
		for (long tick = 25 * DAY; tick <= 30 * DAY; tick++) {
			history.record(tick, season++);
		}
		for (long day = 0; day <= 30; day++) {
			long expected = day <= 5 ? day * DAY : day <= 25 ? stopped : stopped + (day - 25) * DAY;
			assertEquals(expected, history.seasonAt(day * DAY), SeasonHistory.TOLERANCE + SeasonHistory.SPACING,
					"day " + day);
		}
	}

	@Test
	void commandJumpsAreKeptApart() {
		SeasonHistory history = new SeasonHistory();
		for (long tick = 0; tick <= DAY; tick++) {
			history.record(tick, tick);
		}
		history.recordJump(DAY, DAY, 50 * DAY);
		for (long tick = DAY + 1; tick <= 2 * DAY; tick++) {
			history.record(tick, 49 * DAY + tick);
		}
		assertEquals(DAY / 2, history.seasonAt(DAY / 2));
		assertEquals(50 * DAY, history.seasonAt(DAY));
		assertEquals(50 * DAY + DAY / 2, history.seasonAt(DAY + DAY / 2), 1);
	}

	@Test
	void beforeTheRecordTheYearRanNormally() {
		SeasonHistory history = new SeasonHistory();
		history.record(100 * DAY, 7 * DAY);
		assertEquals(5 * DAY, history.seasonAt(98 * DAY));
	}

	@Test
	void savedPointsComeBack() {
		SeasonHistory history = new SeasonHistory();
		for (long tick = 0; tick <= 3 * DAY; tick += 10) {
			history.record(tick, tick / 2);
		}
		history.recordJump(3 * DAY, 3 * DAY / 2, 0);
		SeasonHistory restored = new SeasonHistory(history.points());
		for (long tick = 0; tick <= 3 * DAY; tick += DAY / 4) {
			assertEquals(history.seasonAt(tick), restored.seasonAt(tick), "tick " + tick);
		}
	}

	@Test
	void brokenSavedPointsAreDropped() {
		SeasonHistory restored = new SeasonHistory(List.of(new long[]{100, 5}, new long[]{50, 7}, new long[]{1}, new long[]{200, 105}));
		assertEquals(55, restored.seasonAt(150));
	}

	@Test
	void gameTimeGoingBackStartsAgain() {
		SeasonHistory history = new SeasonHistory();
		for (long tick = 1000; tick <= 5000; tick++) {
			history.record(tick, tick);
		}
		history.record(10, 999_999);
		assertEquals(999_999, history.seasonAt(10));
		assertEquals(999_999 - 5, history.seasonAt(5));
	}

	@Test
	void historyStaysSmallOverALongTime() {
		SeasonHistory history = new SeasonHistory();
		long season = 0;
		for (long tick = 0; tick < 400 * DAY; tick += 20) {
			// Long days and short nights: the year moves unevenly within each day
			boolean day = (tick % DAY) < DAY / 2;
			season += day ? 16 : 24;
			history.record(tick, season);
		}
		assertTrue(history.size() <= SeasonHistory.MAX_POINTS, "had " + history.size());
	}
}
