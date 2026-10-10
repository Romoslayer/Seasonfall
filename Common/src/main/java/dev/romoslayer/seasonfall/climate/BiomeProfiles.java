package dev.romoslayer.seasonfall.climate;

import com.mojang.serialization.DynamicOps;
import dev.romoslayer.seasonfall.Seasonfall;
import dev.romoslayer.seasonfall.config.SeasonfallConfig;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import java.util.Collections;
import java.util.List;
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

	// The loaders' shared biome tags, which modded biomes usually join. On 1.20.1 Fabric fills in "c" tags and Forge (and
	// NeoForge 47) its own "forge" ones, under different names, so a biome counts if it is in either. Forge has no icy,
	// deciduous, jungle or savanna tree tags on this version.
	private static final List<TagKey<Biome>> SNOWY = List.of(tag("c", "snowy"), tag("forge", "is_snowy"));
	private static final List<TagKey<Biome>> ICY = List.of(tag("c", "icy"));
	private static final List<TagKey<Biome>> DESERT = List.of(tag("c", "desert"), tag("forge", "is_desert"));
	private static final List<TagKey<Biome>> SWAMP = List.of(tag("c", "swamp"), tag("forge", "is_swamp"));
	private static final List<TagKey<Biome>> CONIFEROUS = List.of(tag("c", "tree_coniferous"), tag("forge", "is_coniferous"));
	private static final List<TagKey<Biome>> DECIDUOUS = List.of(tag("c", "tree_deciduous"));
	private static final List<TagKey<Biome>> JUNGLE_TREES = List.of(tag("c", "tree_jungle"));
	private static final List<TagKey<Biome>> SAVANNA_TREES = List.of(tag("c", "tree_savanna"));

	private BiomeProfiles() {
	}

	public static BiomeProfile profile(Holder<Biome> holder, DynamicOps<Tag> ops) {
		Biome biome = holder.value();
		float temperature = biome.getBaseTemperature();
		float downfall = downfall(biome, ops);
		boolean precipitation = biome.hasPrecipitation();
		BiomeStyle style = derive(holder, temperature, downfall, precipitation);

		SeasonfallConfig.BiomeOverride override = holder.unwrapKey()
				.map(key -> SeasonfallConfig.get().biomeOverrides.get(key.location().toString()))
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
		if (temperature < FREEZING || in(biome, SNOWY) || in(biome, ICY)) {
			return BiomeStyle.FROZEN;
		}
		if (biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_DEEP_OCEAN)) {
			return BiomeStyle.OCEAN;
		}
		if (biome.is(BiomeTags.IS_JUNGLE) || in(biome, JUNGLE_TREES) || (temperature >= 0.95F && downfall >= 0.8F)) {
			return BiomeStyle.TROPICAL;
		}
		if (biome.is(BiomeTags.IS_SAVANNA) || in(biome, SAVANNA_TREES) || (precipitation && temperature >= 1.0F)) {
			return BiomeStyle.SAVANNA;
		}
		if (biome.is(BiomeTags.IS_BADLANDS) || in(biome, DESERT) || (!precipitation && temperature >= 1.0F)) {
			return BiomeStyle.ARID;
		}
		if (biome.is(BiomeTags.IS_TAIGA) || in(biome, CONIFEROUS)) {
			return BiomeStyle.EVERGREEN;
		}
		if (in(biome, SWAMP) || (downfall >= 0.85F && temperature >= 0.6F)) {
			return BiomeStyle.WETLAND;
		}
		if (biome.is(BiomeTags.IS_FOREST) || in(biome, DECIDUOUS)) {
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
			Tag encoded = Biome.NETWORK_CODEC.encodeStart(ops, biome).getOrThrow(false, error -> {
			});
			return encoded instanceof CompoundTag tag && tag.contains("downfall", Tag.TAG_ANY_NUMERIC) ? tag.getFloat("downfall") : 0.5F;
		} catch (RuntimeException e) {
			Seasonfall.LOGGER.warn("Could not read a biome's downfall, assuming 0.5: {}", e.getMessage());
			return 0.5F;
		}
	}

	private static float clamp(@Nullable Float value, float fallback) {
		return value == null || !Float.isFinite(value) ? fallback : Math.max(0.0F, value);
	}

	private static boolean in(Holder<Biome> biome, List<TagKey<Biome>> tags) {
		for (TagKey<Biome> tag : tags) {
			if (biome.is(tag)) {
				return true;
			}
		}
		return false;
	}

	private static TagKey<Biome> tag(String namespace, String path) {
		return TagKey.create(Registries.BIOME, new ResourceLocation(namespace, path));
	}
}
