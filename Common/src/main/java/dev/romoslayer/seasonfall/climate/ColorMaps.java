package dev.romoslayer.seasonfall.climate;

/**
 * The grass and leaf colours a biome would normally get from the game's colour maps, worked out without them: a
 * dedicated server has no textures. The colour maps are smooth over their triangle, so a quadratic patch through six
 * sample colours (the three corners and the three edge midpoints) lands within a few RGB steps of them everywhere.
 */
public final class ColorMaps {
	// Corner and edge-midpoint colours, sampled once from each colour map:
	// A = hot and wet, B = hot and dry, C = cold, then the midpoints of A-B, B-C and A-C
	private static final int[] GRASS = {0x47CD33, 0xBFB755, 0x80B497, 0x82C245, 0xA0B676, 0x7CBD6B};
	private static final int[] FOLIAGE = {0x1ABF00, 0xAEA42A, 0x60A17B, 0x64B216, 0x87A353, 0x5BAB46};

	/**
	 * Swamp grass ignores the biome's grass colour: the game picks one of two fixed colours from noise. Seasonal swamp
	 * grass is worked out from their average, and a client with Seasonfall shifts both by the same difference.
	 */
	public static final int SWAMP_GRASS = 0x5B733A;

	private ColorMaps() {
	}

	/** {@code color} moved by the difference between {@code to} and {@code from}, each channel kept within 0 to 255. */
	public static int shift(int color, int from, int to) {
		int r = channel((color >> 16 & 255) + (to >> 16 & 255) - (from >> 16 & 255));
		int g = channel((color >> 8 & 255) + (to >> 8 & 255) - (from >> 8 & 255));
		int b = channel((color & 255) + (to & 255) - (from & 255));
		return r << 16 | g << 8 | b;
	}

	private static int channel(int value) {
		return Math.max(0, Math.min(255, value));
	}

	/** An 0xRRGGBB colour as the fully opaque 0xAARRGGBB the game's colour methods return. */
	public static int opaque(int rgb) {
		return 0xFF000000 | rgb;
	}

	public static int grass(float temperature, float downfall) {
		return sample(GRASS, temperature, downfall);
	}

	public static int foliage(float temperature, float downfall) {
		return sample(FOLIAGE, temperature, downfall);
	}

	/** {@code from} moved {@code amount} (0 to 1) of the way to {@code to}, as 0xRRGGBB. */
	public static int blend(int from, int to, float amount) {
		if (amount <= 0.0F) {
			return from & 0xFFFFFF;
		}
		amount = Math.min(1.0F, amount);
		int r = Math.round(lerp(from >> 16 & 255, to >> 16 & 255, amount));
		int g = Math.round(lerp(from >> 8 & 255, to >> 8 & 255, amount));
		int b = Math.round(lerp(from & 255, to & 255, amount));
		return r << 16 | g << 8 | b;
	}

	private static float lerp(int from, int to, float amount) {
		return from + (to - from) * amount;
	}

	private static int sample(int[] nodes, float temperature, float downfall) {
		// The same mapping the game uses: x from temperature, y from downfall scaled by temperature
		double t = clamp(temperature);
		double wet = clamp(downfall) * t;
		double a = wet;
		double b = t - wet;
		double c = 1.0 - t;
		double[] weights = {
				a * (2 * a - 1), b * (2 * b - 1), c * (2 * c - 1),
				4 * a * b, 4 * b * c, 4 * a * c
		};
		int rgb = 0;
		for (int shift = 16; shift >= 0; shift -= 8) {
			double value = 0;
			for (int i = 0; i < nodes.length; i++) {
				value += weights[i] * (nodes[i] >> shift & 255);
			}
			rgb |= (int) Math.round(Math.max(0, Math.min(255, value))) << shift;
		}
		return rgb;
	}

	private static double clamp(float value) {
		return Math.max(0.0, Math.min(1.0, value));
	}
}
