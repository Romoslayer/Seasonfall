package dev.romoslayer.seasonfall.crop;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.romoslayer.seasonfall.config.SeasonfallConfig;
import java.util.Random;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

class GrowthTicksTest {
	/** Ticks a plant actually gets per tick the game gives it, on average over many seeded draws. */
	private static double averageTicks(float multiplier) {
		Random random = new Random(1234);
		int draws = 400_000;
		long ticks = 0;
		for (int i = 0; i < draws; i++) {
			if (GrowthTicks.runs(multiplier, random.nextFloat())) {
				ticks += 1 + GrowthTicks.extra(multiplier, random.nextFloat());
			}
		}
		return (double) ticks / draws;
	}

	@ParameterizedTest
	@ValueSource(floats = {0.0F, 0.5F, 1.0F, 1.3F, 2.0F, 3.0F, 4.0F})
	void ticksMatchTheReportedSpeed(float multiplier) {
		assertEquals(GrowthTicks.effective(multiplier), averageTicks(multiplier), 0.01);
	}

	@Test
	void speedIsCappedTheSameWayEverywhere() {
		assertEquals(SeasonfallConfig.MAX_GROWTH, GrowthTicks.effective(9.0F));
		assertEquals(SeasonfallConfig.MAX_GROWTH, averageTicks(9.0F), 0.01);
		assertEquals(0.0F, GrowthTicks.effective(-2.0F));
		assertEquals(1.0F, GrowthTicks.effective(Float.NaN));
	}

	@Test
	void zeroNeverTicksAndOneNeverAddsTicks() {
		assertEquals(false, GrowthTicks.runs(0.0F, 0.0F));
		assertEquals(0, GrowthTicks.extra(1.0F, 0.0F));
		assertEquals(2, GrowthTicks.extra(3.0F, 0.999F));
	}

	@Test
	void seasonStrengthScalesTowardsNormal() {
		// Strength 0 means no seasonal effect at all, even for a biome's own fixed speeds
		assertEquals(1.0F, GrowthTicks.scaled(0.2F, 0.0F));
		assertEquals(0.6F, GrowthTicks.scaled(0.2F, 0.5F), 1e-6);
		assertEquals(0.2F, GrowthTicks.scaled(0.2F, 1.0F), 1e-6);
	}
}
