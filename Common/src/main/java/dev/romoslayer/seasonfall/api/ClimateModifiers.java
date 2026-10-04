package dev.romoslayer.seasonfall.api;

/**
 * Seasonal climate at a place, for a weather mod (such as Stormcell) to build its own weather on. Seasonfall decides
 * the long-term climate; it never creates weather itself.
 *
 * @param temperatureOffset          added to the biome's normal temperature (Minecraft's scale, where rain turns to snow
 *                                   below 0.15)
 * @param humidityMultiplier         how humid the air is compared to normal for the biome
 * @param precipitationMultiplier    how much rain or snow falls compared to normal
 * @param stormProbabilityMultiplier how likely storms are compared to normal
 */
public record ClimateModifiers(float temperatureOffset, float humidityMultiplier, float precipitationMultiplier, float stormProbabilityMultiplier) {
	/** No seasonal influence: what a place without seasons reports. */
	public static final ClimateModifiers NEUTRAL = new ClimateModifiers(0.0F, 1.0F, 1.0F, 1.0F);
}
