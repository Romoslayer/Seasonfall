package dev.romoslayer.seasonfall.climate;

import com.mojang.serialization.DynamicOps;
import dev.romoslayer.seasonfall.Seasonfall;
import dev.romoslayer.seasonfall.config.Palette;
import dev.romoslayer.seasonfall.config.SeasonValues;
import dev.romoslayer.seasonfall.config.SeasonfallConfig;
import dev.romoslayer.seasonfall.network.BiomeSeasonPayload;
import dev.romoslayer.seasonfall.time.SeasonClock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistrySynchronization;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ARGB;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;

/**
 * What players see of the season. Every joining player gets each seasonal biome's temperature (which decides whether
 * their game draws rain or snow) and seasonal grass and leaf colours inside the biome data sent while joining; an
 * unmodified game only takes these then. Players with Seasonfall installed also get {@link BiomeSeasonPayload} updates
 * while they play.
 * <p>
 * Colours follow the year in a fixed number of steps, so everyone joining during the same step sees the same thing.
 */
public final class BiomeSync {
	private static final String TEMPERATURE = "temperature";
	private static final String EFFECTS = "effects";
	private static final String GRASS = "grass_color";
	private static final String FOLIAGE = "foliage_color";
	private static final String DRY_FOLIAGE = "dry_foliage_color";

	/**
	 * How much of the season shows on leaves whose colour is in their texture (pale oak): none in spring and
	 * summer, then the biome's autumn and winter colours laid over the texture.
	 */
	private static final SeasonValues TEXTURED_LEAVES = new SeasonValues(0.0F, 0.0F, 1.0F, 0.8F);
	/**
	 * Cherry leaves are nearly all blossom, so a strong autumn colour turns them red. They warm towards gold in autumn
	 * instead.
	 */
	private static final int CHERRY_GOLD = 0xFFD04A;
	private static final SeasonValues CHERRY_GOLD_AMOUNT = new SeasonValues(0.0F, 0.0F, 0.55F, 0.0F);
	/**
	 * An overlay only darkens, and a darker pink or green is still pink or green. So in winter cherry also gets a cool
	 * tint that cancels its pink (the texture averages #E5ADC2), leaving a soft pink-grey, and fades less than other
	 * leaves so it does not turn ashen next to the snow.
	 */
	private static final int CHERRY_GREY = 0xC1FFE3;
	private static final SeasonValues CHERRY_GREY_AMOUNT = new SeasonValues(0.0F, 0.0F, 0.0F, 0.7F);
	private static final SeasonValues CHERRY_FADE = new SeasonValues(0.0F, 0.0F, 0.0F, 0.3F);
	/**
	 * The same for azalea, whose green (the texture averages #5A732C) gets a pink tint instead. Its texture has too little
	 * blue to reach grey, so it goes a dormant olive-brown, darkened less than pale oak so it does not turn murky. Flowering
	 * azalea gets exactly the same colour all year: the two are mixed in one tree, and anything gentler left those trees
	 * patchy in autumn.
	 */
	private static final int AZALEA_DORMANT = 0xFFC8FF;
	private static final SeasonValues AZALEA_DORMANT_AMOUNT = new SeasonValues(0.0F, 0.0F, 0.0F, 1.0F);
	private static final SeasonValues AZALEA_LEAVES = new SeasonValues(0.0F, 0.0F, 1.0F, 0.6F);
	private static final int WHITE = 0xFFFFFF;

	private BiomeSync() {
	}

	/**
	 * How one biome looks to players: the temperature their game uses for rain or snow, and its colours (0xRRGGBB).
	 * Birch and spruce leaves only change for players with Seasonfall installed: an unmodified game colours them the same
	 * everywhere, whatever it is sent.
	 */
	public record Look(float temperature, int grass, int foliage, int dryFoliage, int birch, int spruce, int leafOverlay, int azaleaOverlay,
			int cherryOverlay) {
	}

	/**
	 * The season's look of a biome.
	 *
	 * @param colorPhase  the point in the year the colours are for
	 * @param temperature the temperature to send (season included)
	 */
	public static Look look(Biome biome, BiomeProfile profile, float colorPhase, float temperature) {
		SeasonfallConfig config = SeasonfallConfig.get();
		Look normal = normal(biome, profile.baseTemperature(), profile.downfall());
		int grass = normal.grass();
		int foliage = normal.foliage();
		int birch = normal.birch();
		int spruce = normal.spruce();
		int leafOverlay = normal.leafOverlay();
		int azaleaOverlay = normal.azaleaOverlay();
		int cherryOverlay = normal.cherryOverlay();
		Palette palette = palette(profile.style());
		if (config.visuals.grassColorChanges) {
			grass = tint(grass, palette.grassColors(), palette.grassStrengths(), colorPhase, profile.grassChange());
			// The dark forest effect averages the grass colour with a fixed dark green, which halves the season. Sending
			// half as much again shows three quarters of it; the full change on that dark base turned muddy brown.
			if (biome.getSpecialEffects().grassColorModifier() == BiomeSpecialEffects.GrassColorModifier.DARK_FOREST) {
				grass = ColorMaps.blend(grass, ColorMaps.shift(grass, normal.grass(), grass), 0.5F);
			}
		}
		if (config.visuals.foliageColorChanges) {
			foliage = tint(foliage, palette.foliageColors(), palette.foliageStrengths(), colorPhase, profile.foliageChange());
			// Birch is a broadleaf tree like the biome's own; spruce keeps its needles wherever it grows
			birch = tint(birch, palette.foliageColors(), palette.foliageStrengths(), colorPhase, profile.foliageChange());
			Palette evergreen = palette(BiomeStyle.EVERGREEN);
			spruce = tint(spruce, evergreen.foliageColors(), evergreen.foliageStrengths(), colorPhase,
					profile.seasonStrength() * BiomeStyle.EVERGREEN.foliageChange());
			leafOverlay = tint(WHITE, palette.foliageColors(), palette.foliageStrengths(), colorPhase,
					profile.foliageChange() * TEXTURED_LEAVES.at(colorPhase));
			int dormant = ColorMaps.blend(WHITE, AZALEA_DORMANT, profile.foliageChange() * AZALEA_DORMANT_AMOUNT.at(colorPhase));
			azaleaOverlay = multiply(dormant, tint(WHITE, palette.foliageColors(), palette.foliageStrengths(), colorPhase,
					profile.foliageChange() * AZALEA_LEAVES.at(colorPhase)));
			int gold = ColorMaps.blend(WHITE, CHERRY_GOLD, profile.foliageChange() * CHERRY_GOLD_AMOUNT.at(colorPhase));
			int grey = ColorMaps.blend(WHITE, CHERRY_GREY, profile.foliageChange() * CHERRY_GREY_AMOUNT.at(colorPhase));
			cherryOverlay = multiply(multiply(grey, gold), tint(WHITE, palette.foliageColors(), palette.foliageStrengths(), colorPhase,
					profile.foliageChange() * CHERRY_FADE.at(colorPhase)));
		}
		// Seasonal snow switched off on the server: players should see the rain that actually falls
		boolean seasonalTemperature = config.visuals.sendSeasonalTemperature && config.snow.seasonalSnowPersistence;
		return new Look(seasonalTemperature ? temperature : profile.baseTemperature(), grass, foliage, normal.dryFoliage(), birch, spruce, leafOverlay,
				azaleaOverlay, cherryOverlay);
	}

	/**
	 * How a biome looks without any season: its own temperature, and the colours it sets for itself or the client would
	 * otherwise work out. Sent explicitly, because the colours a player was sent while joining may be seasonal ones.
	 */
	public static Look normal(Biome biome, float temperature, float downfall) {
		BiomeSpecialEffects effects = biome.getSpecialEffects();
		int grass = effects.grassColorModifier() == BiomeSpecialEffects.GrassColorModifier.SWAMP ? ColorMaps.SWAMP_GRASS
				: effects.grassColorOverride().map(rgb -> rgb & 0xFFFFFF).orElseGet(() -> ColorMaps.grass(temperature, downfall));
		int foliage = effects.foliageColorOverride().map(rgb -> rgb & 0xFFFFFF).orElseGet(() -> ColorMaps.foliage(temperature, downfall));
		int dryFoliage = effects.dryFoliageColorOverride().map(rgb -> rgb & 0xFFFFFF).orElseGet(() -> ColorMaps.dryFoliage(temperature, downfall));
		return new Look(temperature, grass, foliage, dryFoliage, FoliageColor.FOLIAGE_BIRCH & 0xFFFFFF, FoliageColor.FOLIAGE_EVERGREEN & 0xFFFFFF, WHITE, WHITE,
				WHITE);
	}

	// ---- While joining

	public static List<RegistrySynchronization.PackedRegistryEntry> rewrite(DynamicOps<Tag> ops, RegistryAccess registries,
			List<RegistrySynchronization.PackedRegistryEntry> entries) {
		SeasonClock clock = Seasonfall.clock();
		SeasonfallConfig config = SeasonfallConfig.get();
		if (clock == null || !config.general.enabled || !config.visuals.enabled) {
			return entries;
		}
		Registry<Biome> biomes = registries.lookupOrThrow(Registries.BIOME);
		float colorPhase = stagePhase(clock.yearProgress(), config.visuals.foliageStages);
		List<RegistrySynchronization.PackedRegistryEntry> rewritten = new ArrayList<>(entries.size());
		for (RegistrySynchronization.PackedRegistryEntry entry : entries) {
			Biome biome = biomes.getValue(entry.id());
			BiomeProfile profile = biome == null ? null : Climate.profile(biome);
			if (profile == null) {
				rewritten.add(entry);
				continue;
			}
			try {
				// The temperature right now, so a player who just joined agrees with the server about rain or snow
				Look look = look(biome, profile, colorPhase, profile.baseTemperature() + Climate.offset(biome));
				rewritten.add(new RegistrySynchronization.PackedRegistryEntry(entry.id(), Optional.of(encode(ops, biome, entry.data(), look))));
			} catch (RuntimeException e) {
				Seasonfall.LOGGER.error("Could not send the seasonal version of biome {}; sending it unchanged", entry.id(), e);
				rewritten.add(entry);
			}
		}
		return rewritten;
	}

	private static Tag encode(DynamicOps<Tag> ops, Biome biome, Optional<Tag> data, Look look) {
		Tag encoded = data.map(Tag::copy).orElseGet(() -> Biome.NETWORK_CODEC.encodeStart(ops, biome).getOrThrow());
		if (!(encoded instanceof CompoundTag tag)) {
			throw new IllegalStateException("biome data is not an NBT compound");
		}
		tag.putFloat(TEMPERATURE, look.temperature());
		CompoundTag effects = tag.getCompoundOrEmpty(EFFECTS);
		putColor(ops, effects, GRASS, look.grass());
		putColor(ops, effects, FOLIAGE, look.foliage());
		putColor(ops, effects, DRY_FOLIAGE, look.dryFoliage());
		tag.put(EFFECTS, effects);
		return tag;
	}

	private static void putColor(DynamicOps<Tag> ops, CompoundTag effects, String key, int rgb) {
		effects.put(key, ExtraCodecs.STRING_RGB_COLOR.encodeStart(ops, ARGB.opaque(rgb)).getOrThrow());
	}

	// ---- While playing (players with Seasonfall installed)

	/**
	 * Every biome's look, in pages: the season's look for biomes with seasons, and their normal look for every other
	 * biome (or for all of them when {@code seasonal} is false), so whatever a player was sent before is replaced.
	 * Temperatures are for the same step of the year as the colours, so updates only change when the step does.
	 */
	public static List<BiomeSeasonPayload> livePayloads(MinecraftServer server, float colorPhase, boolean seasonal) {
		DynamicOps<Tag> ops = server.registryAccess().createSerializationContext(NbtOps.INSTANCE);
		List<BiomeSeasonPayload.Entry> entries = new ArrayList<>();
		server.registryAccess().lookupOrThrow(Registries.BIOME).listElements().forEach(holder -> {
			Look look = liveLook(holder, colorPhase, seasonal, ops);
			entries.add(new BiomeSeasonPayload.Entry(holder.key().identifier(), look.temperature(), look.grass(), look.foliage(), look.dryFoliage(),
					look.birch(), look.spruce(), look.leafOverlay(), look.azaleaOverlay(), look.cherryOverlay()));
		});
		List<BiomeSeasonPayload> pages = new ArrayList<>();
		for (int start = 0; start < entries.size(); start += BiomeSeasonPayload.MAX_PAGE_SIZE) {
			pages.add(new BiomeSeasonPayload(List.copyOf(entries.subList(start, Math.min(entries.size(), start + BiomeSeasonPayload.MAX_PAGE_SIZE)))));
		}
		return pages;
	}

	private static Look liveLook(Holder.Reference<Biome> holder, float colorPhase, boolean seasonal, DynamicOps<Tag> ops) {
		Biome biome = holder.value();
		BiomeProfile profile = Climate.profile(biome);
		if (seasonal && profile != null) {
			return look(biome, profile, colorPhase, profile.baseTemperature() + Climate.temperatureOffset(profile, colorPhase));
		}
		return profile != null ? normal(biome, profile.baseTemperature(), profile.downfall())
				: normal(biome, biome.getBaseTemperature(), BiomeProfiles.downfall(biome, ops));
	}

	// ---- Steps

	/** The middle of the colour step the year is in, for a year split into {@code stages} steps. */
	public static float stagePhase(float yearProgress, int stages) {
		return (stage(yearProgress, stages) + 0.5F) / stages;
	}

	public static int stage(float yearProgress, int stages) {
		return Math.floorMod((int) Math.floor(yearProgress * stages), stages);
	}

	private static Palette palette(BiomeStyle style) {
		Map<String, Palette> palettes = SeasonfallConfig.get().palettes;
		Palette palette = palettes.get(style.paletteId());
		return palette != null ? palette : palettes.get(BiomeStyle.TEMPERATE.id());
	}

	/** Two overlay colours applied one after the other, as a single colour. */
	private static int multiply(int a, int b) {
		return ((a >> 16 & 255) * (b >> 16 & 255) / 255) << 16 | ((a >> 8 & 255) * (b >> 8 & 255) / 255) << 8 | (a & 255) * (b & 255) / 255;
	}

	/** Blends the palette's tint for this point in the year over a colour. */
	public static int tint(int base, int[] colors, float[] strengths, float phase, float strength) {
		// Each palette entry sits in the middle of its third of a season
		int color = Keyframes.cyclicColor(colors, 0.5F, phase);
		float amount = Keyframes.cyclic(strengths, 0.5F, phase) * strength;
		return ColorMaps.blend(base, color, amount);
	}
}
