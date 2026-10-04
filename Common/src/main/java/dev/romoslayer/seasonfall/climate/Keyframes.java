package dev.romoslayer.seasonfall.climate;

/** Smooth interpolation between evenly spaced values that repeat every year. */
public final class Keyframes {
	private Keyframes() {
	}

	/**
	 * @param values values spread evenly over the year
	 * @param offset where the first value sits, in steps: 0 = at the very start of the year, 0.5 = half a step in
	 * @param phase  point in the year, 0 to 1 (wraps around)
	 */
	public static float cyclic(float[] values, float offset, float phase) {
		int count = values.length;
		float position = phase * count - offset;
		int index = (int) Math.floor(position);
		float blend = ease(position - index);
		float from = values[Math.floorMod(index, count)];
		float to = values[Math.floorMod(index + 1, count)];
		return from + (to - from) * blend;
	}

	/** The same for four values (one per season), without allocating an array. */
	public static float cyclic(float first, float second, float third, float fourth, float offset, float phase) {
		float position = phase * 4.0F - offset;
		int index = (int) Math.floor(position);
		float blend = ease(position - index);
		float from = pick(Math.floorMod(index, 4), first, second, third, fourth);
		float to = pick(Math.floorMod(index + 1, 4), first, second, third, fourth);
		return from + (to - from) * blend;
	}

	private static float pick(int index, float first, float second, float third, float fourth) {
		return switch (index) {
			case 0 -> first;
			case 1 -> second;
			case 2 -> third;
			default -> fourth;
		};
	}

	/** The same for colours: each channel of 0xRRGGBB on its own. */
	public static int cyclicColor(int[] colors, float offset, float phase) {
		int count = colors.length;
		float position = phase * count - offset;
		int index = (int) Math.floor(position);
		float blend = ease(position - index);
		int from = colors[Math.floorMod(index, count)];
		int to = colors[Math.floorMod(index + 1, count)];
		return ColorMaps.blend(from, to, blend);
	}

	/** Eases in and out of each value, so there is no visible kink as the year passes one. */
	private static float ease(float t) {
		return (1.0F - (float) Math.cos(Math.PI * t)) * 0.5F;
	}
}
