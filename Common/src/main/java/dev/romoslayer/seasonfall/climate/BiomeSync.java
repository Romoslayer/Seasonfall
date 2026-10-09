package dev.romoslayer.seasonfall.climate;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
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
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
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

	/**
	 * How much of the season shows on leaves whose colour is in their texture (azalea): none in spring and summer, then
	 * the biome's autumn and winter colours laid over the texture.
	 */
	private static final SeasonValues TEXTURED_LEAVES = new SeasonValues(0.0F, 0.0F, 1.0F, 0.8F);
	/**
	 * Blossoming leaves (cherry, flowering azalea) have their flowers in the same texture, so a strong autumn colour turns
	 * them red. They warm towards gold in autumn instead, and only fade a little in winter.
	 */
	private static final int BLOSSOM_GOLD = 0xFFD04A;
	private static final SeasonValues BLOSSOM_GOLD_AMOUNT = new SeasonValues(0.0F, 0.0F, 0.55F, 0.15F);
	private static final SeasonValues BLOSSOM_FADE = new SeasonValues(0.0F, 0.0F, 0.0F, 0.5F);
	private static final int WHITE = 0xFFFFFF;

	private BiomeSync() {
	}

	/**
	 * How one biome looks to players: the temperature their game uses for rain or snow, and its colours (0xRRGGBB).
	 * Birch and spruce leaves only change for players with Seasonfall installed: an unmodified game colours them the same
	 * everywhere, whatever it is sent.
	 */
	public record Look(float temperature, int grass, int foliage, int birch, int spruce, int leafOverlay, int blossomOverlay) {
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
		int blossomOverlay = normal.blossomOverlay();
		Palette palette = palette(profile.style());
		if (config.visuals.grassColorChanges) {
			grass = tint(grass, palette.grassColors(), palette.grassStrengths(), colorPhase, profile.grassChange());
			// The dark forest effect averages the grass colour with a fixed dark green, which halves the season. Sending
			// half as much again shows three quarters of it; the full change on that dark base turned muddy brown.
			if (biome.getSpecialEffects().getGrassColorModifier() == BiomeSpecialEffects.GrassColorModifier.DARK_FOREST) {
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
			int faded = tint(WHITE, palette.foliageColors(), palette.foliageStrengths(), colorPhase,
					profile.foliageChange() * BLOSSOM_FADE.at(colorPhase));
			blossomOverlay = multiply(faded, ColorMaps.blend(WHITE, BLOSSOM_GOLD, profile.foliageChange() * BLOSSOM_GOLD_AMOUNT.at(colorPhase)));
		}
		// Seasonal snow switched off on the server: players should see the rain that actually falls
		boolean seasonalTemperature = config.visuals.sendSeasonalTemperature && config.snow.seasonalSnowPersistence;
		return new Look(seasonalTemperature ? temperature : profile.baseTemperature(), grass, foliage, birch, spruce, leafOverlay, blossomOverlay);
	}

	/**
	 * How a biome looks without any season: its own temperature, and the colours it sets for itself or the client would
	 * otherwise work out. Sent explicitly, because the colours a player was sent while joining may be seasonal ones.
	 */
	public static Look normal(Biome biome, float temperature, float downfall) {
		BiomeSpecialEffects effects = biome.getSpecialEffects();
		int grass = effects.getGrassColorModifier() == BiomeSpecialEffects.GrassColorModifier.SWAMP ? ColorMaps.SWAMP_GRASS
				: effects.getGrassColorOverride().map(rgb -> rgb & 0xFFFFFF).orElseGet(() -> ColorMaps.grass(temperature, downfall));
		int foliage = effects.getFoliageColorOverride().map(rgb -> rgb & 0xFFFFFF).orElseGet(() -> ColorMaps.foliage(temperature, downfall));
		return new Look(temperature, grass, foliage, FoliageColor.getBirchColor() & 0xFFFFFF, FoliageColor.getEvergreenColor() & 0xFFFFFF, WHITE,
				WHITE);
	}

	// ---- While joining

	/**
	 * The biome codec of the registry data in the login packet, which writes the season into every biome it encodes.
	 * Decoding (on a client with Seasonfall) is left to the game's own codec.
	 */
	public static Codec<Biome> joinCodec(Codec<Biome> codec) {
		return new Codec<>() {
			@Override
			public <T> DataResult<Pair<Biome, T>> decode(DynamicOps<T> ops, T input) {
				return codec.decode(ops, input);
			}

			@Override
			@SuppressWarnings("unchecked")
			public <T> DataResult<T> encode(Biome biome, DynamicOps<T> ops, T prefix) {
				return codec.encode(biome, ops, prefix).map(encoded -> encoded instanceof Tag tag ? (T) seasonal(biome, tag) : encoded);
			}

			@Override
			public String toString() {
				return "Seasonal[" + codec + "]";
			}
		};
	}

	/**
	 * A biome's encoding for a joining player, with the season's temperature and colours where it has seasons. The game
	 * encodes the login packet on a network thread, so this only reads what the server thread last worked out.
	 */
	private static Tag seasonal(Biome biome, Tag encoded) {
		SeasonClock clock = Seasonfall.clock();
		SeasonfallConfig config = SeasonfallConfig.get();
		BiomeProfile profile = Climate.profile(biome);
		if (clock == null || !config.general.enabled || !config.visuals.enabled || profile == null || !(encoded instanceof CompoundTag original)) {
			return encoded;
		}
		try {
			// The temperature right now, so a player who just joined agrees with the server about rain or snow
			Look look = look(biome, profile, stagePhase(clock.yearProgress(), config.visuals.foliageStages),
					profile.baseTemperature() + Climate.offset(biome));
			CompoundTag tag = original.copy();
			tag.putFloat(TEMPERATURE, look.temperature());
			// A new, empty compound if it is missing
			CompoundTag effects = tag.getCompound(EFFECTS);
			effects.putInt(GRASS, look.grass());
			effects.putInt(FOLIAGE, look.foliage());
			tag.put(EFFECTS, effects);
			return tag;
		} catch (RuntimeException e) {
			Seasonfall.LOGGER.error("Could not send the seasonal version of a biome; sending it unchanged", e);
			return encoded;
		}
	}

	// ---- While playing (players with Seasonfall installed)

	/**
	 * Every biome's look, in pages: the season's look for biomes with seasons, and their normal look for every other
	 * biome (or for all of them when {@code seasonal} is false), so whatever a player was sent before is replaced.
	 * Temperatures are for the same step of the year as the colours, so updates only change when the step does.
	 */
	public static List<BiomeSeasonPayload> livePayloads(MinecraftServer server, float colorPhase, boolean seasonal) {
		DynamicOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, server.registryAccess());
		List<BiomeSeasonPayload.Entry> entries = new ArrayList<>();
		server.registryAccess().registryOrThrow(Registries.BIOME).holders().forEach(holder -> {
			Look look = liveLook(holder, colorPhase, seasonal, ops);
			entries.add(new BiomeSeasonPayload.Entry(holder.key().location(), look.temperature(), look.grass(), look.foliage(), look.birch(),
					look.spruce(), look.leafOverlay(), look.blossomOverlay()));
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
