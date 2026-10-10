package dev.romoslayer.seasonfall.climate;

import org.jspecify.annotations.Nullable;

/** Added to every {@link net.minecraft.world.level.biome.Biome} by {@code BiomeMixin}. */
public interface SeasonalBiome {
	/** No colour sent: the biome works its colour out as usual. */
	int NO_COLOR = -1;

	// ---- Server side

	/** This biome's seasonal profile, or null when it has no seasons (not in a dimension with seasons). */
	@Nullable BiomeProfile seasonfall$profile();

	/** How much warmer (below zero: colder) than normal the biome is right now. */
	float seasonfall$temperatureOffset();

	void seasonfall$setSeason(@Nullable BiomeProfile profile, float temperatureOffset);

	// ---- Client side, only for players with Seasonfall installed (see BiomeSeasonPayload)

	/** Change from the temperature the biome was synced with when joining. */
	float seasonfall$clientTemperatureChange();

	int seasonfall$clientGrass();

	int seasonfall$clientFoliage();

	/** Birch leaves (which the game otherwise colours the same everywhere). */
	int seasonfall$clientBirch();

	/** Spruce leaves (which the game otherwise colours the same everywhere). */
	int seasonfall$clientSpruce();

	/**
	 * A colour multiplied over azalea and flowering azalea leaves, whose colour comes from their texture: white leaves
	 * them as they are. They go olive-brown in winter.
	 */
	int seasonfall$clientAzaleaOverlay();

	/** The same for cherry leaves: the gentler overlay, and grey in winter. */
	int seasonfall$clientCherryOverlay();

	void seasonfall$setClientSeason(float temperatureChange, int grass, int foliage, int birch, int spruce, int azaleaOverlay, int cherryOverlay);
}
