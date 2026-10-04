package dev.romoslayer.seasonfall.climate;

import dev.romoslayer.seasonfall.config.SeasonValues;
import org.jspecify.annotations.Nullable;

/**
 * How one biome goes through the year. The strengths here already include the biome's overall season strength.
 *
 * @param baseTemperature        the biome's own temperature, without seasons
 * @param downfall               the biome's own downfall (humidity)
 * @param temperatureSeasonality multiplies the yearly temperature curve
 * @param foliageChange          how much of the palette's leaf tint applies
 * @param grassChange            how much of the palette's grass tint applies
 * @param cropSeasonality        how much the season changes crop growth here
 * @param weatherSeasonality     how much the seasonal weather tendencies apply here
 * @param seasonStrength         the biome's overall season strength (0 = no seasons, 1 = normal)
 * @param allowSeasonalSnow      whether the season may bring snow and ice where the biome normally has none
 * @param cropMultipliers        fixed crop growth speeds for this biome, or null to use the crop groups
 */
public record BiomeProfile(BiomeStyle style, float baseTemperature, float downfall, boolean hasPrecipitation,
		float temperatureSeasonality, float foliageChange, float grassChange, float cropSeasonality, float weatherSeasonality,
		float seasonStrength, boolean allowSeasonalSnow, @Nullable SeasonValues cropMultipliers) {
}
