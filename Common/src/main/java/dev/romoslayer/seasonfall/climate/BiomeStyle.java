package dev.romoslayer.seasonfall.climate;

import java.util.Locale;
import org.jspecify.annotations.Nullable;

/**
 * The broad kind of landscape a biome is, which sets how it goes through the year: its palette and how strongly each
 * seasonal effect applies before any per-biome overrides.
 */
public enum BiomeStyle {
	//         temperature  foliage  grass  crops  weather  snow
	DECIDUOUS(1.0F, 1.0F, 1.0F, 1.0F, 1.0F, true),
	TEMPERATE(1.0F, 0.8F, 1.0F, 1.0F, 1.0F, true),
	EVERGREEN(1.1F, 0.6F, 0.8F, 1.0F, 1.0F, true),
	WETLAND(0.8F, 0.7F, 0.7F, 0.9F, 0.9F, true),
	SAVANNA(0.5F, 0.6F, 0.7F, 0.6F, 0.7F, false),
	ARID(1.0F, 0.5F, 0.5F, 0.5F, 0.5F, false),
	TROPICAL(0.2F, 0.6F, 0.6F, 0.25F, 0.3F, false),
	FROZEN(0.5F, 0.8F, 0.8F, 1.0F, 0.7F, true),
	OCEAN(0.4F, 0.5F, 0.5F, 1.0F, 0.6F, false);

	private final float temperatureSeasonality;
	private final float foliageChange;
	private final float grassChange;
	private final float cropSeasonality;
	private final float weatherSeasonality;
	private final boolean seasonalSnow;

	BiomeStyle(float temperatureSeasonality, float foliageChange, float grassChange, float cropSeasonality, float weatherSeasonality,
			boolean seasonalSnow) {
		this.temperatureSeasonality = temperatureSeasonality;
		this.foliageChange = foliageChange;
		this.grassChange = grassChange;
		this.cropSeasonality = cropSeasonality;
		this.weatherSeasonality = weatherSeasonality;
		this.seasonalSnow = seasonalSnow;
	}

	public String id() {
		return this.name().toLowerCase(Locale.ROOT);
	}

	/** Which palette the style uses (oceans borrow the grassland one for their islands and shores). */
	public String paletteId() {
		return this == OCEAN ? TEMPERATE.id() : this.id();
	}

	public float temperatureSeasonality() {
		return this.temperatureSeasonality;
	}

	public float foliageChange() {
		return this.foliageChange;
	}

	public float grassChange() {
		return this.grassChange;
	}

	public float cropSeasonality() {
		return this.cropSeasonality;
	}

	/** How much the seasonal weather tendencies (humidity, precipitation, storms) apply. */
	public float weatherSeasonality() {
		return this.weatherSeasonality;
	}

	public boolean seasonalSnow() {
		return this.seasonalSnow;
	}

	public static @Nullable BiomeStyle parse(@Nullable String id) {
		if (id == null) {
			return null;
		}
		for (BiomeStyle style : values()) {
			if (style.id().equals(id.toLowerCase(Locale.ROOT))) {
				return style;
			}
		}
		return null;
	}
}
