package dev.romoslayer.seasonfall.client;

import dev.romoslayer.seasonfall.Seasonfall;
import dev.romoslayer.seasonfall.climate.SeasonalBiome;
import dev.romoslayer.seasonfall.network.BiomeSeasonPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;

/**
 * The client side of live updates: takes the season's biome colours and temperatures from the server and redraws the
 * world with them. Only loaded in a game client, and only ever called when the server sends an update.
 */
public final class ClientSeasons {
	private ClientSeasons() {
	}

	public static void apply(BiomeSeasonPayload payload) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null) {
			return;
		}
		Registry<Biome> biomes = level.registryAccess().lookupOrThrow(Registries.BIOME);
		boolean changed = false;
		for (BiomeSeasonPayload.Entry entry : payload.biomes()) {
			Biome biome = biomes.getValue(entry.biome());
			if (biome == null) {
				continue;
			}
			SeasonalBiome seasonal = (SeasonalBiome) (Object) biome;
			// The biome already has the temperature it was sent while joining; this is the change since then
			float temperatureChange = entry.temperature() - biome.getBaseTemperature();
			changed |= seasonal.seasonfall$clientGrass() != entry.grass() || seasonal.seasonfall$clientFoliage() != entry.foliage()
					|| seasonal.seasonfall$clientDryFoliage() != entry.dryFoliage() || seasonal.seasonfall$clientBirch() != entry.birch()
					|| seasonal.seasonfall$clientSpruce() != entry.spruce() || seasonal.seasonfall$clientLeafOverlay() != entry.leafOverlay()
					|| seasonal.seasonfall$clientAzaleaOverlay() != entry.azaleaOverlay()
					|| seasonal.seasonfall$clientCherryOverlay() != entry.cherryOverlay();
			seasonal.seasonfall$setClientSeason(temperatureChange, entry.grass(), entry.foliage(), entry.dryFoliage(), entry.birch(), entry.spruce(),
					entry.leafOverlay(), entry.azaleaOverlay(), entry.cherryOverlay());
		}
		Seasonfall.LOGGER.debug("Live season update for {} biomes{}", payload.biomes().size(), changed ? ", redrawing" : "");
		if (changed) {
			redraw(minecraft, level);
		}
	}

	/** Rebuilds every section in view in place, so the new colours show without the world blinking out. */
	private static void redraw(Minecraft minecraft, ClientLevel level) {
		level.clearTintCaches();
		SectionPos camera = SectionPos.of(minecraft.gameRenderer.mainCamera().position());
		int distance = minecraft.options.getEffectiveRenderDistance() + 1;
		for (int x = camera.x() - distance; x <= camera.x() + distance; x++) {
			for (int z = camera.z() - distance; z <= camera.z() + distance; z++) {
				for (int y = level.getMinSectionY(); y <= level.getMaxSectionY(); y++) {
					minecraft.levelExtractor.setSectionDirty(x, y, z);
				}
			}
		}
	}
}
