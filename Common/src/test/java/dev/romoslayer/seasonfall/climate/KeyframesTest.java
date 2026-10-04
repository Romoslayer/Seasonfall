package dev.romoslayer.seasonfall.climate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.romoslayer.seasonfall.config.SeasonValues;
import org.junit.jupiter.api.Test;

class KeyframesTest {
	@Test
	void valuesSitInTheMiddleOfTheirSeason() {
		SeasonValues values = new SeasonValues(1.15F, 1.1F, 0.85F, 0.4F);
		assertEquals(1.15F, values.at(0.125F), 1e-5);
		assertEquals(1.1F, values.at(0.375F), 1e-5);
		assertEquals(0.85F, values.at(0.625F), 1e-5);
		assertEquals(0.4F, values.at(0.875F), 1e-5);
		// Halfway from winter to spring at the turn of the year, either side of it
		assertEquals((0.4F + 1.15F) / 2, values.at(0.0F), 1e-5);
		assertEquals(values.at(0.0F), values.at(1.0F), 1e-5);
	}

	@Test
	void fourValueVersionMatchesTheArrayVersion() {
		float[] values = {1.0F, 2.0F, -1.0F, 0.5F};
		for (float phase = -0.5F; phase <= 1.5F; phase += 0.01F) {
			assertEquals(Keyframes.cyclic(values, 0.5F, phase), Keyframes.cyclic(values[0], values[1], values[2], values[3], 0.5F, phase), 1e-6);
		}
	}

	@Test
	void theYearHasNoJumps() {
		float[] values = {-0.45F, -0.2F, 0.0F, 0.15F, 0.05F, -0.2F, -0.55F, -0.9F};
		float previous = Keyframes.cyclic(values, 0.0F, 0.0F);
		for (int i = 1; i <= 10000; i++) {
			float now = Keyframes.cyclic(values, 0.0F, i / 10000.0F);
			assertTrue(Math.abs(now - previous) < 0.01F, "jump at " + i);
			previous = now;
		}
	}

	@Test
	void coloursBlendPerChannel() {
		assertEquals(0x808080, ColorMaps.blend(0x000000, 0xFFFFFF, 0.5F), 0x010101);
		assertEquals(0x123456, ColorMaps.blend(0x123456, 0xFFFFFF, 0.0F));
		assertEquals(0xFFFFFF, ColorMaps.blend(0x123456, 0xFFFFFF, 5.0F));
	}
}
