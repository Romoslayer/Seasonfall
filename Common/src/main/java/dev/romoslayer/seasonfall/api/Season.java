package dev.romoslayer.seasonfall.api;

import java.util.Locale;

/** The four seasons, in the order the year runs through them. */
public enum Season {
	SPRING("Spring"),
	SUMMER("Summer"),
	AUTUMN("Autumn"),
	WINTER("Winter");

	private final String displayName;

	Season(String displayName) {
		this.displayName = displayName;
	}

	public String displayName() {
		return this.displayName;
	}

	public String id() {
		return this.name().toLowerCase(Locale.ROOT);
	}

	public Season next() {
		return values()[(this.ordinal() + 1) % values().length];
	}

	/** The point in the year (0 to 1, see {@link SeasonfallApi#yearProgress()}) at which this season starts. */
	public float startPhase() {
		return this.ordinal() * 0.25F;
	}

	public static Season ofPhase(float phase) {
		return values()[Math.floorMod((int) Math.floor(phase * 4.0F), 4)];
	}
}
