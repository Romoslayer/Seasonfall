package dev.romoslayer.seasonfall.climate;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ColorMapsTest {
	@Test
	void shiftMovesByTheDifference() {
		// The swamp's two colours move by the season's change from their average
		assertEquals(0x4C763C, ColorMaps.shift(0x4C763C, ColorMaps.SWAMP_GRASS, ColorMaps.SWAMP_GRASS));
		assertEquals(0x5C7036, ColorMaps.shift(0x4C763C, 0x5B733A, 0x6B6D34));
	}

	@Test
	void shiftStaysInRange() {
		assertEquals(0xFF0000, ColorMaps.shift(0xF00010, 0x000020, 0x400000));
	}

	@Test
	void doubledShiftUndoesTheDarkForestAverage() {
		int normal = 0x79C05A;
		int seasonal = 0x97A24E;
		int sent = ColorMaps.shift(seasonal, normal, seasonal);
		// The game's dark forest effect: ((base & 0xFEFEFE) + 0x28340A) >> 1
		int shown = (sent & 0xFEFEFE) + 0x28340A >> 1;
		int plain = (normal & 0xFEFEFE) + 0x28340A >> 1;
		// The visible change is the full seasonal change, within rounding
		assertEquals((seasonal >> 16 & 255) - (normal >> 16 & 255), (shown >> 16 & 255) - (plain >> 16 & 255), 1);
		assertEquals((seasonal >> 8 & 255) - (normal >> 8 & 255), (shown >> 8 & 255) - (plain >> 8 & 255), 1);
	}
}
