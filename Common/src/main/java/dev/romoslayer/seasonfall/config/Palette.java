package dev.romoslayer.seasonfall.config;

import java.util.Map;

/** Seasonal leaf and grass tints for one biome style, keyed early_spring through late_winter. */
public final class Palette {
	public Map<String, Tint> foliage;
	public Map<String, Tint> grass;

	// Worked out once from the (already validated) tints; not part of the config file
	private transient int[] foliageColors;
	private transient float[] foliageStrengths;
	private transient int[] grassColors;
	private transient float[] grassStrengths;

	public Palette(Map<String, Tint> foliage, Map<String, Tint> grass) {
		this.foliage = foliage;
		this.grass = grass;
	}

	public int[] foliageColors() {
		if (this.foliageColors == null) {
			this.foliageColors = colors(this.foliage);
		}
		return this.foliageColors;
	}

	public float[] foliageStrengths() {
		if (this.foliageStrengths == null) {
			this.foliageStrengths = strengths(this.foliage);
		}
		return this.foliageStrengths;
	}

	public int[] grassColors() {
		if (this.grassColors == null) {
			this.grassColors = colors(this.grass);
		}
		return this.grassColors;
	}

	public float[] grassStrengths() {
		if (this.grassStrengths == null) {
			this.grassStrengths = strengths(this.grass);
		}
		return this.grassStrengths;
	}

	private static int[] colors(Map<String, Tint> tints) {
		int[] colors = new int[tints.size()];
		int i = 0;
		for (Tint tint : tints.values()) {
			colors[i++] = tint.rgb();
		}
		return colors;
	}

	private static float[] strengths(Map<String, Tint> tints) {
		float[] strengths = new float[tints.size()];
		int i = 0;
		for (Tint tint : tints.values()) {
			strengths[i++] = tint.strength;
		}
		return strengths;
	}

	/** A colour and how much of it to blend in (0 to 1). */
	public static final class Tint {
		public String color;
		public float strength;

		public Tint(String color, float strength) {
			this.color = color;
			this.strength = strength;
		}

		public int rgb() {
			return Integer.parseInt(this.color.substring(1), 16) & 0xFFFFFF;
		}

		boolean isValid() {
			return this.color != null && this.color.length() == 7 && this.color.charAt(0) == '#'
					&& this.color.substring(1).chars().allMatch(c -> Character.digit(c, 16) >= 0) && Float.isFinite(this.strength);
		}
	}
}
