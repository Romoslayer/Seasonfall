package dev.romoslayer.seasonfall.climate;

import com.mojang.serialization.DynamicOps;
import dev.romoslayer.seasonfall.Seasonfall;
import dev.romoslayer.seasonfall.config.SeasonfallConfig;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import org.jspecify.annotations.Nullable;

/**
 * Works out each biome's seasonal profile from what the biome itself says about its climate (temperature, downfall,
 * whether it rains) and its tags, so modded biomes get sensible seasons without being listed anywhere. The config's
 * biome overrides then replace whatever parts of it they set.
 */
public final class BiomeProfiles {
	/** Rain turns to snow, and still water freezes, below this temperature. */
	public static final float FREEZING = 0.15F;

	/** Biomes that keep their normal look and climate all year, even in a dimension with seasons. */
	public static final TagKey<Biome> WITHOUT_SEASONS = TagKey.create(Registries.BIOME, Seasonfall.id("without_seasons"));

	// Shared "c" tags that Fabric, NeoForge and Forge all fill in, which modded biomes usually join
	private static final TagKey<Biome> SNOWY = common("is_snowy");
	private static final TagKey<Biome> ICY = common("is_icy");
	private static final TagKey<Biome> DESERT = common("is_desert");
	private static final TagKey<Biome> SWAMP = common("is_swamp");
	private static final TagKey<Biome> CONIFEROUS = common("is_tree/coniferous");
	private static final TagKey<Biome> DECIDUOUS = common("is_tree/deciduous");
	private static final TagKey<Biome> JUNGLE_TREES = common("is_tree/jungle");
	private static final TagKey<Biome> SAVANNA_TREES = common("is_tree/savanna");

	private BiomeProfiles() {
	}

	public static BiomeProfile profile(Holder<Biome> holder, DynamicOps<Tag> ops) {
		Biome biome = holder.value();
		float temperature = biome.getBaseTemperature();
		float downfall = downfall(biome, ops);
		boolean precipitation = biome.hasPrecipitation();
		BiomeStyle style = derive(holder, temperature, downfall, precipitation);

		SeasonfallConfig.BiomeOverride override = holder.unwrapKey()
				.map(key -> SeasonfallConfig.get().biomeOverrides.get(key.identifier().toString()))
				.orElse(null);
		if (override == null) {
			return new BiomeProfile(style, temperature, downfall, precipitation, style.temperatureSeasonality(), style.foliageChange(),
					style.grassChange(), style.cropSeasonality(), style.weatherSeasonality(), 1.0F, style.seasonalSnow(), null);
		}
		BiomeStyle chosen = BiomeStyle.parse(override.style);
		if (chosen != null) {
			style = chosen;
		}
		float strength = clamp(override.seasonStrength, 1.0F);
		return new BiomeProfile(style, temperature, downfall, precipitation,
				strength * clamp(override.temperatureSeasonality, style.temperatureSeasonality()),
				strength * clamp(override.foliageChangeStrength, style.foliageChange()),
				strength * clamp(override.grassChangeStrength, style.grassChange()),
				strength * clamp(override.cropSeasonality, style.cropSeasonality()),
				strength * style.weatherSeasonality(),
				strength,
				override.allowSeasonalSnow == null ? style.seasonalSnow() : override.allowSeasonalSnow,
				override.cropMultipliers());
	}

	private static BiomeStyle derive(Holder<Biome> biome, float temperature, float downfall, boolean precipitation) {
		if (temperature < FREEZING || biome.is(SNOWY) || biome.is(ICY)) {
			return BiomeStyle.FROZEN;
		}
		if (biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_DEEP_OCEAN)) {
			return BiomeStyle.OCEAN;
		}
		if (biome.is(BiomeTags.IS_JUNGLE) || biome.is(JUNGLE_TREES) || (temperature >= 0.95F && downfall >= 0.8F)) {
			return BiomeStyle.TROPICAL;
		}
		if (biome.is(BiomeTags.IS_SAVANNA) || biome.is(SAVANNA_TREES) || (precipitation && temperature >= 1.0F)) {
			return BiomeStyle.SAVANNA;
		}
		if (biome.is(BiomeTags.IS_BADLANDS) || biome.is(DESERT) || (!precipitation && temperature >= 1.0F)) {
			return BiomeStyle.ARID;
		}
		if (biome.is(BiomeTags.IS_TAIGA) || biome.is(CONIFEROUS)) {
			return BiomeStyle.EVERGREEN;
		}
		if (biome.is(SWAMP) || (downfall >= 0.85F && temperature >= 0.6F)) {
			return BiomeStyle.WETLAND;
		}
		if (biome.is(BiomeTags.IS_FOREST) || biome.is(DECIDUOUS)) {
			return BiomeStyle.DECIDUOUS;
		}
		if (temperature < 0.35F) {
			// Cold, windswept and stony places: mostly spruce, and long winters
			return BiomeStyle.EVERGREEN;
		}
		return BiomeStyle.TEMPERATE;
	}

	/** Each biome's downfall, read once: it never changes while a world is running. */
	private static final Map<Biome, Float> DOWNFALL = Collections.synchronizedMap(new WeakHashMap<>());

	/** Biomes have no public getter for their downfall, so it is read back from their own network encoding. */
	public static float downfall(Biome biome, DynamicOps<Tag> ops) {
		Float known = DOWNFALL.get(biome);
		if (known != null) {
			return known;
		}
		float downfall = readDownfall(biome, ops);
		DOWNFALL.put(biome, downfall);
		return downfall;
	}

	private static float readDownfall(Biome biome, DynamicOps<Tag> ops) {
		try {
			Tag encoded = Biome.NETWORK_CODEC.encodeStart(ops, biome).getOrThrow();
			return encoded instanceof CompoundTag tag ? tag.getFloatOr("downfall", 0.5F) : 0.5F;
		} catch (RuntimeException e) {
			Seasonfall.LOGGER.warn("Could not read a biome's downfall, assuming 0.5: {}", e.getMessage());
			return 0.5F;
		}
	}

	private static float clamp(@Nullable Float value, float fallback) {
		return value == null || !Float.isFinite(value) ? fallback : Math.max(0.0F, value);
	}

	private static TagKey<Biome> common(String path) {
		return TagKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("c", path));
	}
}
