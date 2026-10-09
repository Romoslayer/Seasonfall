package dev.romoslayer.seasonfall.climate;

import com.mojang.serialization.DynamicOps;
import dev.romoslayer.seasonfall.api.ClimateModifiers;
import dev.romoslayer.seasonfall.config.SeasonfallConfig;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import org.jspecify.annotations.Nullable;

/**
 * Seasonal biome temperatures and climate. The change is applied when the server thread asks a biome for its
 * temperature, which covers snowfall, freezing water and melting. World generation runs on other threads and keeps each
 * biome's normal climate, and so does the client half of a singleplayer game (it gets the season by registry sync).
 */
public final class Climate {
	private static volatile @Nullable Thread serverThread;
	/** Set while the game asks a question that should get each biome's normal answer (see the freezing/snow options). */
	private static final ThreadLocal<Boolean> IGNORE_SEASONS = ThreadLocal.withInitial(() -> false);
	/** Dimensions with seasons, worked out with the profiles. */
	private static volatile Set<ResourceKey<Level>> seasonalDimensions = Set.of();

	private Climate() {
	}

	public static void start(MinecraftServer server) {
		serverThread = server.getRunningThread();
	}

	public static void stop(MinecraftServer server) {
		serverThread = null;
		seasonalDimensions = Set.of();
		biomes(server).listElements().forEach(holder -> seasonal(holder.value()).seasonfall$setSeason(null, 0.0F));
	}

	/** Works out every biome's profile again, from scratch: after a config or data pack reload. */
	public static void rebuildProfiles(MinecraftServer server, float phase) {
		Set<Biome> seasonalBiomes = biomesWithSeasons(server);
		DynamicOps<Tag> ops = server.registryAccess().createSerializationContext(NbtOps.INSTANCE);
		boolean enabled = SeasonfallConfig.get().general.enabled;
		biomes(server).listElements().forEach(holder -> {
			BiomeProfile profile = enabled && seasonalBiomes.contains(holder.value()) && !holder.is(BiomeProfiles.WITHOUT_SEASONS)
					? BiomeProfiles.profile(holder, ops) : null;
			seasonal(holder.value()).seasonfall$setSeason(profile, profile == null ? 0.0F : temperatureOffset(profile, phase));
		});
	}

	/** Moves every biome's temperature along to this point in the year. */
	public static void update(MinecraftServer server, float phase) {
		float curve = curve(phase);
		biomes(server).listElements().forEach(holder -> {
			SeasonalBiome biome = seasonal(holder.value());
			BiomeProfile profile = biome.seasonfall$profile();
			if (profile != null) {
				biome.seasonfall$setSeason(profile, offsetFromCurve(profile, curve));
			}
		});
	}

	/** Biomes that can appear in a dimension with seasons. */
	private static Set<Biome> biomesWithSeasons(MinecraftServer server) {
		Set<Biome> result = Collections.newSetFromMap(new IdentityHashMap<>());
		Set<ResourceKey<Level>> dimensionsWithSeasons = new HashSet<>();
		SeasonfallConfig.Dimensions dimensions = SeasonfallConfig.get().dimensions;
		for (ServerLevel level : server.getAllLevels()) {
			if (dimensions.hasSeasons(level.dimension().identifier().toString())) {
				dimensionsWithSeasons.add(level.dimension());
				for (Holder<Biome> biome : level.getChunkSource().getGenerator().getBiomeSource().possibleBiomes()) {
					result.add(biome.value());
				}
			}
		}
		seasonalDimensions = !SeasonfallConfig.get().general.enabled ? Set.of() : Set.copyOf(dimensionsWithSeasons);
		return result;
	}

	/** Whether this dimension has seasons (and Seasonfall is switched on). */
	public static boolean hasSeasons(ServerLevel level) {
		return seasonalDimensions.contains(level.dimension());
	}

	// ---- Temperature

	/** The yearly temperature curve at this point in the year, for a biome with a full seasonal swing. */
	public static float curve(float phase) {
		return Keyframes.cyclic(SeasonfallConfig.get().temperatureCurve(), 0.0F, phase);
	}

	public static float temperatureOffset(BiomeProfile profile, float phase) {
		return offsetFromCurve(profile, curve(phase));
	}

	/** The same, given the yearly curve's value (worked out once for every biome). */
	private static float offsetFromCurve(BiomeProfile profile, float curve) {
		SeasonfallConfig config = SeasonfallConfig.get();
		if (!config.temperature.temperatureSystemEnabled) {
			return 0.0F;
		}
		float offset = curve * profile.temperatureSeasonality();
		float base = profile.baseTemperature();
		if (base < BiomeProfiles.FREEZING) {
			// Snowy biomes stay below freezing all year
			return Math.min(offset, BiomeProfiles.FREEZING - 0.01F - base);
		}
		if (!profile.allowSeasonalSnow()) {
			// Never cold enough for new snow or ice, nor for what is already there to stop melting
			return Math.max(offset, Math.min(0.0F, config.freezing.thawTemperatureThreshold - base));
		}
		return offset;
	}

	/** The temperature a biome reports to the game, given what it would normally be. */
	public static float adjust(Biome biome, float temperature) {
		SeasonalBiome seasonal = seasonal(biome);
		if (Thread.currentThread() != serverThread) {
			// A client with Seasonfall installed, following live updates (0 everywhere else, world generation included)
			return temperature + seasonal.seasonfall$clientTemperatureChange();
		}
		return IGNORE_SEASONS.get() ? temperature : temperature + seasonal.seasonfall$temperatureOffset();
	}

	/** Whether this is the server thread: the only place seasonal temperatures (and the server's settings) apply. */
	public static boolean isServerThread() {
		return Thread.currentThread() == serverThread;
	}

	/**
	 * Runs something on the server thread with every biome at its normal temperature. Only meaningful there: other
	 * threads never see the server's seasonal temperatures in the first place.
	 */
	public static <T> T withoutSeasons(Supplier<T> action) {
		boolean previous = IGNORE_SEASONS.get();
		IGNORE_SEASONS.set(true);
		try {
			return action.get();
		} finally {
			IGNORE_SEASONS.set(previous);
		}
	}

	// ---- Lookups

	public static @Nullable BiomeProfile profile(Biome biome) {
		return seasonal(biome).seasonfall$profile();
	}

	public static @Nullable BiomeProfile profile(Holder<Biome> biome) {
		return profile(biome.value());
	}

	public static float offset(Biome biome) {
		return seasonal(biome).seasonfall$temperatureOffset();
	}

	/** The seasonal climate of a biome at this point in the year, for weather mods. */
	public static ClimateModifiers modifiers(Biome biome, float phase) {
		BiomeProfile profile = profile(biome);
		SeasonfallConfig config = SeasonfallConfig.get();
		if (profile == null || !config.general.enabled) {
			return ClimateModifiers.NEUTRAL;
		}
		SeasonfallConfig.Stormcell share = config.stormcell;
		float strength = profile.weatherSeasonality();
		return new ClimateModifiers(
				share.seasonalTemperatureModifiers ? offset(biome) : 0.0F,
				share.seasonalHumidityModifiers ? scaled(config.weather.humidity.at(phase), strength) : 1.0F,
				share.seasonalPrecipitationModifiers ? scaled(config.weather.precipitation.at(phase), strength) : 1.0F,
				share.seasonalStormModifiers ? scaled(config.weather.storms.at(phase), strength) : 1.0F);
	}

	private static float scaled(float multiplier, float strength) {
		return Math.max(0.0F, 1.0F + (multiplier - 1.0F) * strength);
	}

	private static Registry<Biome> biomes(MinecraftServer server) {
		return server.registryAccess().lookupOrThrow(Registries.BIOME);
	}

	private static SeasonalBiome seasonal(Biome biome) {
		return (SeasonalBiome) (Object) biome;
	}
}
