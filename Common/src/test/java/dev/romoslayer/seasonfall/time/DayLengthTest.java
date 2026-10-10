package dev.romoslayer.seasonfall.time;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** With the default config: summer days 1.25 times normal, winter days 0.75 times. */
class DayLengthTest {
	private static final float MID_SPRING = 0.125F;
	private static final float MID_SUMMER = 0.375F;
	private static final float MID_AUTUMN = 0.625F;
	private static final float MID_WINTER = 0.875F;

	@Test
	void equalDayAndNightAtTheEquinoxes() {
		assertEquals(0.5F, DayLength.daytimeFraction(MID_SPRING), 1.0E-6F);
		assertEquals(0.5F, DayLength.daytimeFraction(MID_AUTUMN), 1.0E-6F);
	}

	@Test
	void fractionIsHalfTheDaytimeMultiplier() {
		assertEquals(0.625F, DayLength.daytimeFraction(MID_SUMMER), 1.0E-6F);
		assertEquals(0.375F, DayLength.daytimeFraction(MID_WINTER), 1.0E-6F);
		for (float year = 0.0F; year < 1.0F; year += 0.01F) {
			assertEquals(0.5F * DayLength.daytimeMultiplier(year), DayLength.daytimeFraction(year), 1.0E-6F);
		}
	}

	@Test
	void changesSmoothlyThroughTheYear() {
		for (float year = 0.0F; year < 1.0F; year += 0.001F) {
			float step = Math.abs(DayLength.daytimeFraction(year + 0.001F) - DayLength.daytimeFraction(year));
			assertTrue(step < 0.002F, "jump of " + step + " at " + year);
		}
	}
}
