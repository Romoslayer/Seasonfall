package dev.romoslayer.seasonfall.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import dev.romoslayer.seasonfall.api.Season;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * config/seasonfall.json. Anything missing or invalid falls back to its default, and the file is rewritten on every
 * load so that options added by an update appear in it, with their explanations.
 */
public final class SeasonfallConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private static final Logger LOGGER = LoggerFactory.getLogger("Seasonfall");
	private static final String FILE_NAME = "seasonfall.json";
	/** The fastest any plant may grow, as a multiple of normal: up to three extra growth ticks. */
	public static final float MAX_GROWTH = 4.0F;
	/** Longest a season may be, in days. */
	public static final int MAX_SEASON_DAYS = 10000;

	private static @Nullable SeasonfallConfig instance;
	private static @Nullable Path file;

	@Comment("Seasonfall runs entirely on the server: players join with a normal, unmodded game.")
	public General general = new General();
	public SeasonLengths seasonLength = new SeasonLengths();
	@Comment("Which dimensions have seasons. Dimensions not listed use \"otherDimensions\".")
	public Dimensions dimensions = new Dimensions();
	public Temperature temperature = new Temperature();
	public Visuals visuals = new Visuals();
	public Crops crops = new Crops();
	public Freezing freezing = new Freezing();
	public Snow snow = new Snow();
	public DayLength dayLength = new DayLength();
	public Weather weather = new Weather();
	public Stormcell stormcell = new Stormcell();
	public Performance performance = new Performance();
	@Comment("""
			Change how individual biomes (vanilla or modded, by id) respond to the seasons. Every biome gets a profile worked out
			from its own climate and tags; anything set here replaces that part of it. Possible settings:
			  style: deciduous, temperate, evergreen, wetland, savanna, arid, tropical, frozen or ocean (picks the colour palette)
			  seasonStrength: scales every seasonal effect (0 = no seasons at all, 1 = normal)
			  temperatureSeasonality: how far the temperature swings over the year (1 = a temperate plain)
			  foliageChangeStrength, grassChangeStrength: how strongly leaves and grass change colour
			  cropSeasonality: how much the season matters to crops here (0 = not at all)
			  allowSeasonalSnow: whether the seasons may make it cold enough here for snow and ice
			  springCropMultiplier, summerCropMultiplier, autumnCropMultiplier, winterCropMultiplier: fixed crop growth speeds""")
	public Map<String, BiomeOverride> biomeOverrides = defaultBiomeOverrides();
	@Comment("""
			Seasonal tints for each style, from early spring to late winter. The colour is blended over the biome's own colour by
			"strength" (0 to 1), which is then scaled by the biome's foliage or grass change strength. The year moves smoothly
			between these points.""")
	public Map<String, Palette> palettes = Palettes.defaults();

	// ---- Sections

	public static final class General {
		@Comment("Turns the whole mod off without removing it.")
		public boolean enabled = true;
		@Comment("Whether the year moves on with the overworld's time of day. When false, the season stays where it is.")
		public boolean seasonCycleEnabled = true;
		@Comment("""
				Hold the year while nobody is online, so the seasons only pass while people are playing. The server itself keeps
				running with nobody on.""")
		public boolean pauseWhenEmpty = true;
		@Comment("The season a new world starts in.")
		public String startingSeason = Season.SPRING.id();
	}

	public static final class SeasonLengths {
		@Comment("Length of each season in in-game days. The year is the four added together.")
		public int springLengthDays = 24;
		public int summerLengthDays = 24;
		public int autumnLengthDays = 24;
		public int winterLengthDays = 24;

		public int days(Season season) {
			return switch (season) {
				case SPRING -> this.springLengthDays;
				case SUMMER -> this.summerLengthDays;
				case AUTUMN -> this.autumnLengthDays;
				case WINTER -> this.winterLengthDays;
			};
		}
	}

	public static final class Dimensions {
		public Map<String, Boolean> enabled = defaultDimensions();
		public boolean otherDimensions = false;

		public boolean hasSeasons(String dimension) {
			Boolean value = this.enabled.get(dimension);
			return value == null ? this.otherDimensions : value;
		}
	}

	public static final class Temperature {
		@Comment("Seasonal temperatures: snowfall and freezing in the cold months, a thaw in spring.")
		public boolean temperatureSystemEnabled = true;
		@Comment("""
				Temperature change through the year for a biome with a full seasonal swing (a temperate plain), at the start and
				middle of each season. Each biome scales it by its temperatureSeasonality. On Minecraft's scale plains are 0.8 and
				rain turns to snow below 0.15.""")
		public Map<String, Float> curve = defaultTemperatureCurve();
	}

	public static final class Visuals {
		@Comment("""
				Send joining players the season's biome temperatures and colours. An unmodified game only accepts these while
				joining, so players see the latest season each time they connect.""")
		public boolean enabled = true;
		@Comment("Players see snow falling where the season makes it cold enough (otherwise they see rain there).")
		public boolean sendSeasonalTemperature = true;
		public boolean foliageColorChanges = true;
		public boolean grassColorChanges = true;
		@Comment("How many colour steps the year is divided into. More steps means smoother changes between visits.")
		public int foliageStages = 16;
		@Comment("""
				Players who also have Seasonfall installed see colours and snowfall change while they play, without rejoining.
				Players without it are not affected and can always join.""")
		public boolean liveUpdatesForModdedClients = true;
		@Comment("How many colour steps the year has for those players. Each step redraws the world around them once.")
		public int liveUpdateStages = 96;
	}

	public static final class Crops {
		@Comment("Seasons speed up or slow down crop and plant growth.")
		public boolean cropSeasonEffects = true;
		@Comment("""
				Growth speed for each group in the middle of each season (1 = normal, 0.5 = half, 1.2 = 20% faster); it changes
				smoothly in between. Which crops are in which group is set by the block tags seasonfall:crops/warm_season,
				seasonfall:crops/cool_season, seasonfall:crops/default and seasonfall:vegetation (checked in that order).""")
		public Map<String, SeasonValues> groups = defaultCropGroups();
		@Comment("Give single crops (vanilla or modded, by block id) a group name, or growth speeds of their own.")
		public Map<String, JsonElement> cropOverrides = defaultCropOverrides();
		@Comment("Crops under a glass roof (the seasonfall:greenhouse_glass block tag) grow at least at normal speed all year.")
		public boolean greenhouses = true;
		public int greenhouseMaxHeight = 16;
	}

	public static final class Freezing {
		@Comment("""
				Still water freezes where the season makes it cold enough, the same way it does in snowy biomes: gradually, from
				the edges in. Exposed ice melts back into water once it warms up again.""")
		public boolean seasonalFreezing = true;
		@Comment("""
				Water freezes (and snow settles) below 0.15, as in vanilla. Ice and snow only melt once it is this warm, so nothing
				flickers between frozen and thawed around the freezing point.""")
		public float thawTemperatureThreshold = 0.2F;
	}

	public static final class Snow {
		@Comment("""
				Rain falls as snow and settles where the season makes it cold enough. Once it warms up, snow open to the sky
				melts a layer at a time, as often as the game would melt it next to a torch.""")
		public boolean seasonalSnowPersistence = true;
	}

	public static final class DayLength {
		@Comment("""
				Longer days in summer and longer nights in winter, by running the overworld's time of day slower or faster. A day
				and night together still take the usual time.""")
		public boolean seasonalDayLength = true;
		@Comment("How long daytime lasts in midsummer and midwinter compared to normal. Spring and autumn are about even.")
		public float summerDayLengthMultiplier = 1.25F;
		public float winterDayLengthMultiplier = 0.75F;
	}

	public static final class Weather {
		@Comment("""
				Without a weather mod, make vanilla rain and thunderstorms more or less frequent through the year, using the
				precipitation and storm values below. Switched off automatically while Stormcell is installed.""")
		public boolean vanillaWeatherAdjustments = true;
		@Comment("Seasonal tendencies in the middle of each season (1 = normal). Each biome scales them by its seasonStrength.")
		public SeasonValues humidity = new SeasonValues(1.1F, 0.95F, 1.0F, 0.9F);
		public SeasonValues precipitation = new SeasonValues(1.2F, 0.85F, 1.05F, 0.9F);
		public SeasonValues storms = new SeasonValues(1.1F, 1.5F, 0.9F, 0.5F);
	}

	public static final class Stormcell {
		@Comment("Share the seasonal climate with Stormcell when it is installed. Seasonfall never creates weather itself.")
		public boolean stormcellIntegration = true;
		public boolean seasonalTemperatureModifiers = true;
		public boolean seasonalHumidityModifiers = true;
		public boolean seasonalPrecipitationModifiers = true;
		public boolean seasonalStormModifiers = true;
	}

	public static final class Performance {
		@Comment("How often (in ticks) biome temperatures follow the year along. The year is smooth, so this can be slow.")
		public int seasonUpdateIntervalTicks = 100;
		@Comment("How often (in ticks) biome profiles and crop groups are rebuilt anyway. /reload already rebuilds them straight away.")
		public int profileRefreshIntervalTicks = 6000;
	}

	public static final class BiomeOverride {
		public @Nullable String style;
		public @Nullable Float seasonStrength;
		public @Nullable Float temperatureSeasonality;
		public @Nullable Float foliageChangeStrength;
		public @Nullable Float grassChangeStrength;
		public @Nullable Float cropSeasonality;
		public @Nullable Boolean allowSeasonalSnow;
		public @Nullable Float springCropMultiplier;
		public @Nullable Float summerCropMultiplier;
		public @Nullable Float autumnCropMultiplier;
		public @Nullable Float winterCropMultiplier;

		public @Nullable SeasonValues cropMultipliers() {
			if (this.springCropMultiplier == null && this.summerCropMultiplier == null && this.autumnCropMultiplier == null
					&& this.winterCropMultiplier == null) {
				return null;
			}
			return new SeasonValues(orOne(this.springCropMultiplier), orOne(this.summerCropMultiplier), orOne(this.autumnCropMultiplier),
					orOne(this.winterCropMultiplier));
		}

		private static float orOne(@Nullable Float value) {
			return value == null || !Float.isFinite(value) ? 1.0F : Math.max(0.0F, Math.min(MAX_GROWTH, value));
		}
	}

	// ---- Loading

	public static SeasonfallConfig get() {
		SeasonfallConfig config = instance;
		if (config == null) {
			// Only before the loader entrypoint has loaded the file
			config = instance = defaults();
		}
		return config;
	}

	public static void load(Path configDir) {
		file = configDir.resolve(FILE_NAME);
		try {
			instance = read(file);
		} catch (IOException | RuntimeException e) {
			LOGGER.error("Could not read {}, using the defaults until it is fixed: {}", file, e.getMessage());
			instance = new SeasonfallConfig().validated(new Problems());
			return;
		}
		write(file, instance);
	}

	/**
	 * Re-reads the file. The new settings only take effect once they have been read and checked in full; if the file
	 * cannot be read at all, the current settings stay. Returns an error message, or null when it loaded.
	 */
	public static @Nullable String reload() {
		if (file == null) {
			return "The config was never loaded";
		}
		SeasonfallConfig candidate;
		try {
			candidate = read(file);
		} catch (IOException | RuntimeException e) {
			return e.getMessage();
		}
		instance = candidate;
		write(file, candidate);
		return null;
	}

	private static SeasonfallConfig read(Path path) throws IOException {
		Problems problems = new Problems();
		SeasonfallConfig config;
		if (!Files.isRegularFile(path)) {
			config = new SeasonfallConfig().validated(problems);
		} else {
			try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
				config = parse(reader, problems);
			}
		}
		for (String problem : problems.messages()) {
			LOGGER.warn("{}: {}", path.getFileName(), problem);
		}
		return config;
	}

	/** Reads and checks a config. Anything unusable is replaced (and reported to {@code problems}). */
	static SeasonfallConfig parse(Reader reader, Problems problems) {
		// The parser accepts the // comments the file is written with
		JsonElement json = JsonParser.parseReader(reader);
		SeasonfallConfig config = json == null || json.isJsonNull() ? null : GSON.fromJson(json, SeasonfallConfig.class);
		return (config == null ? new SeasonfallConfig() : config).validated(problems);
	}

	static String serialize(SeasonfallConfig config) {
		return CommentedJson.write(GSON, config);
	}

	private static void write(Path path, SeasonfallConfig config) {
		try {
			Files.createDirectories(path.getParent());
			Files.writeString(path, serialize(config), StandardCharsets.UTF_8);
		} catch (IOException | RuntimeException e) {
			LOGGER.error("Could not write {}", path, e);
		}
	}

	/** Fills in anything missing and keeps every number finite and within its documented range. */
	SeasonfallConfig validated(Problems problems) {
		SeasonfallConfig defaults = new SeasonfallConfig();
		if (this.general == null) this.general = defaults.general;
		if (this.general.startingSeason == null || parseSeason(this.general.startingSeason) == null) {
			if (this.general.startingSeason != null) {
				problems.add("general.startingSeason must be spring, summer, autumn or winter; using spring");
			}
			this.general.startingSeason = defaults.general.startingSeason;
		}

		if (this.seasonLength == null) this.seasonLength = defaults.seasonLength;
		this.seasonLength.springLengthDays = days(this.seasonLength.springLengthDays, "seasonLength.springLengthDays", problems);
		this.seasonLength.summerLengthDays = days(this.seasonLength.summerLengthDays, "seasonLength.summerLengthDays", problems);
		this.seasonLength.autumnLengthDays = days(this.seasonLength.autumnLengthDays, "seasonLength.autumnLengthDays", problems);
		this.seasonLength.winterLengthDays = days(this.seasonLength.winterLengthDays, "seasonLength.winterLengthDays", problems);

		if (this.dimensions == null) this.dimensions = defaults.dimensions;
		if (this.dimensions.enabled == null) this.dimensions.enabled = defaultDimensions();
		this.dimensions.enabled.values().removeIf(value -> value == null);

		if (this.temperature == null) this.temperature = defaults.temperature;
		this.temperature.curve = completeCurve(this.temperature.curve, problems);

		if (this.visuals == null) this.visuals = defaults.visuals;
		this.visuals.foliageStages = range(this.visuals.foliageStages, 4, 96, "visuals.foliageStages", problems);
		this.visuals.liveUpdateStages = range(this.visuals.liveUpdateStages, 4, 384, "visuals.liveUpdateStages", problems);

		if (this.crops == null) this.crops = defaults.crops;
		Map<String, SeasonValues> defaultGroups = defaultCropGroups();
		Map<String, SeasonValues> groups = new LinkedHashMap<>(defaultGroups);
		if (this.crops.groups != null) {
			this.crops.groups.forEach((name, values) -> {
				if (name != null && values != null) {
					// A season left out of a built-in group keeps that group's default; of a new group, normal speed
					SeasonValues fallback = defaultGroups.getOrDefault(name, SeasonValues.neutral());
					groups.put(name, values.completed(fallback, 0.0F, MAX_GROWTH, "crops.groups." + name, problems));
				}
			});
		}
		this.crops.groups = groups;
		this.crops.cropOverrides = completeCropOverrides(this.crops.cropOverrides, groups, problems);
		this.crops.greenhouseMaxHeight = range(this.crops.greenhouseMaxHeight, 1, 64, "crops.greenhouseMaxHeight", problems);

		if (this.freezing == null) this.freezing = defaults.freezing;
		this.freezing.thawTemperatureThreshold = number(this.freezing.thawTemperatureThreshold, defaults.freezing.thawTemperatureThreshold,
				0.15F, 2.0F, "freezing.thawTemperatureThreshold", problems);
		if (this.snow == null) this.snow = defaults.snow;

		if (this.dayLength == null) this.dayLength = defaults.dayLength;
		this.dayLength.summerDayLengthMultiplier = number(this.dayLength.summerDayLengthMultiplier, defaults.dayLength.summerDayLengthMultiplier,
				0.25F, 1.75F, "dayLength.summerDayLengthMultiplier", problems);
		this.dayLength.winterDayLengthMultiplier = number(this.dayLength.winterDayLengthMultiplier, defaults.dayLength.winterDayLengthMultiplier,
				0.25F, 1.75F, "dayLength.winterDayLengthMultiplier", problems);

		if (this.weather == null) this.weather = defaults.weather;
		this.weather.humidity = seasonValues(this.weather.humidity, defaults.weather.humidity, 0.01F, 10.0F, "weather.humidity", problems);
		this.weather.precipitation = seasonValues(this.weather.precipitation, defaults.weather.precipitation, 0.01F, 10.0F, "weather.precipitation",
				problems);
		this.weather.storms = seasonValues(this.weather.storms, defaults.weather.storms, 0.01F, 10.0F, "weather.storms", problems);
		if (this.stormcell == null) this.stormcell = defaults.stormcell;

		if (this.performance == null) this.performance = defaults.performance;
		this.performance.seasonUpdateIntervalTicks = range(this.performance.seasonUpdateIntervalTicks, 1, 72000,
				"performance.seasonUpdateIntervalTicks", problems);
		this.performance.profileRefreshIntervalTicks = range(this.performance.profileRefreshIntervalTicks, 20, 72000,
				"performance.profileRefreshIntervalTicks", problems);

		this.biomeOverrides = completeBiomeOverrides(this.biomeOverrides, problems);
		this.palettes = Palettes.complete(this.palettes);
		return this;
	}

	private static int days(int value, String path, Problems problems) {
		return range(value, 1, MAX_SEASON_DAYS, path, problems);
	}

	private static int range(int value, int min, int max, String path, Problems problems) {
		if (value < min || value > max) {
			int clamped = Math.max(min, Math.min(max, value));
			problems.add(path + " must be between " + min + " and " + max + "; using " + clamped);
			return clamped;
		}
		return value;
	}

	private static float number(float value, float fallback, float min, float max, String path, Problems problems) {
		if (!Float.isFinite(value)) {
			problems.add(path + " is not a usable number; using " + fallback);
			return fallback;
		}
		if (value < min || value > max) {
			float clamped = Math.max(min, Math.min(max, value));
			problems.add(path + " must be between " + min + " and " + max + "; using " + clamped);
			return clamped;
		}
		return value;
	}

	private static @Nullable Float optionalNumber(@Nullable Float value, float min, float max, String path, Problems problems) {
		if (value == null) {
			return null;
		}
		if (!Float.isFinite(value)) {
			problems.add(path + " is not a usable number; ignoring it");
			return null;
		}
		return number(value, value, min, max, path, problems);
	}

	private static SeasonValues seasonValues(@Nullable SeasonValues values, SeasonValues fallback, float min, float max, String path,
			Problems problems) {
		return values == null ? fallback : values.completed(fallback, min, max, path, problems);
	}

	/** Each crop override is a known group name, or growth speeds of its own (seasons left out grow at normal speed). */
	private static Map<String, JsonElement> completeCropOverrides(@Nullable Map<String, JsonElement> overrides, Map<String, SeasonValues> groups,
			Problems problems) {
		Map<String, JsonElement> result = new LinkedHashMap<>();
		if (overrides == null) {
			return result;
		}
		overrides.forEach((crop, value) -> {
			String path = "crops.cropOverrides." + crop;
			if (crop == null || value == null || value.isJsonNull()) {
				return;
			}
			try {
				if (value.isJsonPrimitive()) {
					if (groups.containsKey(value.getAsString())) {
						result.put(crop, value);
					} else {
						problems.add(path + " names the unknown group \"" + value.getAsString() + "\"; ignoring it");
					}
					return;
				}
				SeasonValues parsed = GSON.fromJson(value, SeasonValues.class);
				SeasonValues completed = (parsed == null ? new SeasonValues() : parsed).completed(SeasonValues.neutral(), 0.0F, MAX_GROWTH, path, problems);
				result.put(crop, GSON.toJsonTree(completed));
			} catch (RuntimeException e) {
				problems.add(path + " is not a group name or growth speeds; ignoring it");
			}
		});
		return result;
	}

	private static Map<String, BiomeOverride> completeBiomeOverrides(@Nullable Map<String, BiomeOverride> overrides, Problems problems) {
		Map<String, BiomeOverride> result = new LinkedHashMap<>();
		if (overrides == null) {
			return result;
		}
		overrides.forEach((biome, override) -> {
			if (biome == null || override == null) {
				return;
			}
			String path = "biomeOverrides." + biome + ".";
			if (override.style != null && dev.romoslayer.seasonfall.climate.BiomeStyle.parse(override.style) == null) {
				problems.add(path + "style \"" + override.style + "\" is not a style; keeping the biome's own");
				override.style = null;
			}
			override.seasonStrength = optionalNumber(override.seasonStrength, 0.0F, 4.0F, path + "seasonStrength", problems);
			override.temperatureSeasonality = optionalNumber(override.temperatureSeasonality, 0.0F, 4.0F, path + "temperatureSeasonality", problems);
			override.foliageChangeStrength = optionalNumber(override.foliageChangeStrength, 0.0F, 4.0F, path + "foliageChangeStrength", problems);
			override.grassChangeStrength = optionalNumber(override.grassChangeStrength, 0.0F, 4.0F, path + "grassChangeStrength", problems);
			override.cropSeasonality = optionalNumber(override.cropSeasonality, 0.0F, 4.0F, path + "cropSeasonality", problems);
			override.springCropMultiplier = optionalNumber(override.springCropMultiplier, 0.0F, MAX_GROWTH, path + "springCropMultiplier", problems);
			override.summerCropMultiplier = optionalNumber(override.summerCropMultiplier, 0.0F, MAX_GROWTH, path + "summerCropMultiplier", problems);
			override.autumnCropMultiplier = optionalNumber(override.autumnCropMultiplier, 0.0F, MAX_GROWTH, path + "autumnCropMultiplier", problems);
			override.winterCropMultiplier = optionalNumber(override.winterCropMultiplier, 0.0F, MAX_GROWTH, path + "winterCropMultiplier", problems);
			result.put(biome, override);
		});
		return result;
	}

	/** A config with every default, checked as if read from a file. */
	public static SeasonfallConfig defaults() {
		return new SeasonfallConfig().validated(new Problems());
	}
	// ---- Lookups

	public Season startingSeason() {
		Season season = parseSeason(this.general.startingSeason);
		return season == null ? Season.SPRING : season;
	}

	public static @Nullable Season parseSeason(String id) {
		for (Season season : Season.values()) {
			if (season.id().equals(id.toLowerCase(Locale.ROOT))) {
				return season;
			}
		}
		return null;
	}

	/** Temperature curve keys, at the start and middle of each season: phases 0, 1/8, 2/8 and so on. */
	public static final String[] CURVE_KEYS = {
			"spring_start", "spring_mid", "summer_start", "summer_mid", "autumn_start", "autumn_mid", "winter_start", "winter_mid"
	};

	public float[] temperatureCurve() {
		float[] values = new float[CURVE_KEYS.length];
		for (int i = 0; i < values.length; i++) {
			values[i] = this.temperature.curve.get(CURVE_KEYS[i]);
		}
		return values;
	}

	// ---- Defaults

	private static Map<String, Boolean> defaultDimensions() {
		Map<String, Boolean> dimensions = new LinkedHashMap<>();
		dimensions.put("minecraft:overworld", true);
		dimensions.put("minecraft:the_nether", false);
		dimensions.put("minecraft:the_end", false);
		return dimensions;
	}

	private static Map<String, Float> defaultTemperatureCurve() {
		float[] values = {-0.45F, -0.2F, 0.0F, 0.15F, 0.05F, -0.2F, -0.55F, -0.9F};
		Map<String, Float> curve = new LinkedHashMap<>();
		for (int i = 0; i < CURVE_KEYS.length; i++) {
			curve.put(CURVE_KEYS[i], values[i]);
		}
		return curve;
	}

	private static Map<String, Float> completeCurve(@Nullable Map<String, Float> curve, Problems problems) {
		Map<String, Float> defaults = defaultTemperatureCurve();
		Map<String, Float> result = new LinkedHashMap<>();
		for (String key : CURVE_KEYS) {
			Float value = curve == null ? null : curve.get(key);
			result.put(key, value == null ? defaults.get(key) : number(value, defaults.get(key), -3.0F, 3.0F, "temperature.curve." + key, problems));
		}
		return result;
	}

	private static Map<String, SeasonValues> defaultCropGroups() {
		Map<String, SeasonValues> groups = new LinkedHashMap<>();
		groups.put("default", new SeasonValues(1.15F, 1.1F, 0.85F, 0.4F));
		groups.put("warm_season", new SeasonValues(1.0F, 1.3F, 0.7F, 0.15F));
		groups.put("cool_season", new SeasonValues(1.25F, 0.85F, 1.15F, 0.55F));
		groups.put("vegetation", new SeasonValues(1.25F, 1.05F, 0.8F, 0.5F));
		return groups;
	}

	private static Map<String, JsonElement> defaultCropOverrides() {
		Map<String, JsonElement> overrides = new LinkedHashMap<>();
		overrides.put("minecraft:sweet_berry_bush", new JsonPrimitive("warm_season"));
		return overrides;
	}

	private static Map<String, BiomeOverride> defaultBiomeOverrides() {
		Map<String, BiomeOverride> overrides = new LinkedHashMap<>();
		BiomeOverride mangrove = new BiomeOverride();
		mangrove.style = "tropical";
		overrides.put("minecraft:mangrove_swamp", mangrove);
		return overrides;
	}

	static float clamp01(float value) {
		return Float.isFinite(value) ? Math.max(0.0F, Math.min(1.0F, value)) : 0.0F;
	}
}
