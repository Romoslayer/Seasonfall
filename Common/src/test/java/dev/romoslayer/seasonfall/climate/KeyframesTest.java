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
	void heldValuesCoverMostOfTheirSeason() {
		SeasonValues values = new SeasonValues(1.15F, 1.1F, 0.85F, 0.0F);
		// Winter is 0.75 to 1: with 80% held, 0.775 to 0.975 is winter's own value
		assertEquals(0.0F, values.heldAt(0.78F, 0.8F), 1e-6);
		assertEquals(0.0F, values.heldAt(0.875F, 0.8F), 1e-6);
		assertEquals(0.0F, values.heldAt(0.97F, 0.8F), 1e-6);
		assertEquals(1.1F, values.heldAt(0.3F, 0.8F), 1e-6);
		// Halfway between two seasons at each turn of the season, either side of the year's end
		assertEquals(0.85F / 2, values.heldAt(0.75F, 0.8F), 1e-5);
		assertEquals(1.15F / 2, values.heldAt(0.0F, 0.8F), 1e-5);
		assertEquals(values.heldAt(0.0F, 0.8F), values.heldAt(1.0F, 0.8F), 1e-5);
		// Nothing held: the same as the plain smooth curve
		for (float phase = 0.0F; phase < 1.0F; phase += 0.01F) {
			assertEquals(values.at(phase), values.heldAt(phase, 0.0F), 1e-5);
		}
	}

	@Test
	void heldValuesHaveNoJumps() {
		SeasonValues values = new SeasonValues(1.15F, 4.0F, 0.85F, 0.0F);
		float previous = values.heldAt(0.0F, 0.8F);
		for (int i = 1; i <= 10000; i++) {
			float now = values.heldAt(i / 10000.0F, 0.8F);
			assertTrue(Math.abs(now - previous) < 0.01F, "jump at " + i);
			previous = now;
		}
	}

	@Test
	void winterAtZeroBarelyGrowsAllWinter() {
		SeasonValues values = new SeasonValues(1.15F, 1.1F, 0.85F, 0.0F);
		double sum = 0;
		int samples = 10000;
		for (int i = 0; i < samples; i++) {
			sum += values.heldAt(0.75F + 0.25F * i / samples, 0.8F);
		}
		// Only the blends at either end of winter grow at all (it was about 0.18 when only midwinter reached 0)
		assertTrue(sum / samples < 0.04, "winter average " + sum / samples);
	}

	@Test
	void coloursBlendPerChannel() {
		assertEquals(0x808080, ColorMaps.blend(0x000000, 0xFFFFFF, 0.5F), 0x010101);
		assertEquals(0x123456, ColorMaps.blend(0x123456, 0xFFFFFF, 0.0F));
		assertEquals(0xFFFFFF, ColorMaps.blend(0x123456, 0xFFFFFF, 5.0F));
	}
}
