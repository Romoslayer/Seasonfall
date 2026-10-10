package dev.romoslayer.seasonfall.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import dev.romoslayer.seasonfall.api.Season;
import java.io.StringReader;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SeasonfallConfigTest {
	private static SeasonfallConfig parse(String json, Problems problems) {
		return SeasonfallConfig.parse(new StringReader(json), problems);
	}

	@Test
	void defaultsHaveNoProblems() {
		Problems problems = new Problems();
		SeasonfallConfig config = parse("{}", problems);
		assertTrue(problems.isEmpty(), problems.messages().toString());
		assertEquals(24, config.seasonLength.springLengthDays);
	}

	@Test
	void pauseWhenEmptyIsOnUnlessTurnedOff() {
		assertTrue(parse("{}", new Problems()).general.pauseWhenEmpty);
		// A file from before the option existed still gets it
		assertTrue(parse("{ general: { enabled: true } }", new Problems()).general.pauseWhenEmpty);
		assertFalse(parse("{ general: { pauseWhenEmpty: false } }", new Problems()).general.pauseWhenEmpty);
	}

	@Test
	void startingSeasonIsRandomByDefault() {
		SeasonfallConfig config = parse("{}", new Problems());
		assertEquals(SeasonfallConfig.RANDOM_SEASON, config.general.startingSeason);
		// The same seed always starts in the same season, and different seeds reach all four
		assertEquals(config.startingSeason(12345L), config.startingSeason(12345L));
		Set<Season> seen = EnumSet.noneOf(Season.class);
		for (long seed = 0; seed < 200; seed++) {
			seen.add(config.startingSeason(seed));
		}
		assertEquals(EnumSet.allOf(Season.class), seen);
	}

	@Test
	void fixedStartingSeasonIgnoresTheSeed() {
		SeasonfallConfig config = parse("{ general: { startingSeason: \"Autumn\" } }", new Problems());
		for (long seed = 0; seed < 50; seed++) {
			assertEquals(Season.AUTUMN, config.startingSeason(seed));
		}
		Problems problems = new Problems();
		parse("{ general: { startingSeason: \"RANDOM\" } }", problems);
		assertTrue(problems.isEmpty(), problems.messages().toString());
	}

	@Test
	void unknownStartingSeasonBecomesRandom() {
		Problems problems = new Problems();
		SeasonfallConfig config = parse("{ general: { startingSeason: \"monsoon\" } }", problems);
		assertEquals(SeasonfallConfig.RANDOM_SEASON, config.general.startingSeason);
		assertFalse(problems.isEmpty());
	}

	@Test
	void writtenFileReadsBackTheSame() {
		SeasonfallConfig defaults = SeasonfallConfig.defaults();
		String written = SeasonfallConfig.serialize(defaults);
		assertTrue(written.contains("// "), "comments are written");
		Problems problems = new Problems();
		SeasonfallConfig again = parse(written, problems);
		assertTrue(problems.isEmpty(), problems.messages().toString());
		assertEquals(SeasonfallConfig.serialize(defaults), SeasonfallConfig.serialize(again));
	}

	@Test
	void partialCropOverrideGrowsNormallyInTheSeasonsLeftOut() {
		SeasonfallConfig config = parse("{ crops: { cropOverrides: { \"minecraft:wheat\": { winter: 0.5 } } } }", new Problems());
		SeasonValues wheat = new Gson().fromJson(config.crops.cropOverrides.get("minecraft:wheat"), SeasonValues.class);
		assertEquals(new SeasonValues(1.0F, 1.0F, 1.0F, 0.5F), wheat);
	}

	@Test
	void partialGroupKeepsItsDefaults() {
		SeasonfallConfig config = parse("{ crops: { groups: { default: { winter: 0.1 }, mine: { summer: 2 } } } }", new Problems());
		assertEquals(new SeasonValues(1.15F, 1.1F, 0.85F, 0.1F), config.crops.groups.get("default"));
		assertEquals(new SeasonValues(1.0F, 2.0F, 1.0F, 1.0F), config.crops.groups.get("mine"));
	}

	@Test
	void hugeAndBrokenNumbersAreReplacedAndReported() {
		Problems problems = new Problems();
		SeasonfallConfig config = parse("""
				{
				  seasonLength: { springLengthDays: 2147483647, summerLengthDays: 0 },
				  dayLength: { summerDayLengthMultiplier: 1e100, winterDayLengthMultiplier: -3 },
				  freezing: { thawTemperatureThreshold: 1e100 },
				  crops: { groups: { default: { spring: 1e100, winter: -1 } }, greenhouseMaxHeight: 100000,
				           cropOverrides: { "minecraft:carrots": "no_such_group", "minecraft:potatoes": { summer: 50 } } },
				  temperature: { curve: { winter_mid: -1e100 } },
				  biomeOverrides: { "minecraft:plains": { seasonStrength: 1e100, winterCropMultiplier: 9, style: "lava" } }
				}
				""", problems);
		assertEquals(SeasonfallConfig.MAX_SEASON_DAYS, config.seasonLength.springLengthDays);
		assertEquals(1, config.seasonLength.summerLengthDays);
		assertEquals(1.25F, config.dayLength.summerDayLengthMultiplier);
		assertEquals(0.25F, config.dayLength.winterDayLengthMultiplier);
		assertEquals(0.2F, config.freezing.thawTemperatureThreshold);
		assertEquals(1.15F, config.crops.groups.get("default").spring);
		assertEquals(0.0F, config.crops.groups.get("default").winter);
		assertEquals(64, config.crops.greenhouseMaxHeight);
		assertFalse(config.crops.cropOverrides.containsKey("minecraft:carrots"));
		assertEquals(SeasonfallConfig.MAX_GROWTH, new Gson().fromJson(config.crops.cropOverrides.get("minecraft:potatoes"), SeasonValues.class).summer);
		assertEquals(-0.9F, config.temperature.curve.get("winter_mid"));
		SeasonfallConfig.BiomeOverride plains = config.biomeOverrides.get("minecraft:plains");
		assertNull(plains.seasonStrength);
		assertEquals(SeasonfallConfig.MAX_GROWTH, plains.winterCropMultiplier);
		assertNull(plains.style);
		assertTrue(problems.messages().size() >= 10, problems.messages().toString());
		// And it can still be written out
		SeasonfallConfig.serialize(config);
	}

	@Test
	void everyNumberIsFiniteAfterChecking() {
		SeasonfallConfig config = parse("{ weather: { storms: { summer: NaN } }, palettes: { deciduous: { foliage: { mid_autumn: { color: \"#FF0000\", strength: 1e100 } } } } }",
				new Problems());
		assertTrue(Float.isFinite(config.weather.storms.summer));
		// Not a finite number at all: the default tint for that spot
		assertEquals(0.8F, config.palettes.get("deciduous").foliage.get("mid_autumn").strength);
	}

	private static final String[] OLD_DECIDUOUS_GRASS = {"#9DB86A:0.3", "#7FC44E:0.2", "#6CC23A:0.15", "#5AA83A:0.1", "#6E9E3A:0.15", "#A5A548:0.2",
			"#B4A64C:0.25", "#B08E48:0.35", "#9A7E50:0.45", "#8E8670:0.45", "#9AA39A:0.5", "#8F9A78:0.4"};
	private static final String[] OLD_DECIDUOUS_FOLIAGE = {"#A7C46A:0.35", "#8CCB4E:0.25", "#6CC23A:0.2", "#4A9E2E:0.15", "#3F8A2A:0.2",
			"#8FA436:0.2", "#E0C828:0.55", "#FF8A10:0.8", "#E2502A:0.82", "#A0703F:0.65", "#7C7666:0.55", "#87866A:0.5"};

	private static String deciduous(String kind, String[] tints) {
		StringBuilder json = new StringBuilder("{ palettes: { deciduous: { " + kind + ": {");
		for (int i = 0; i < tints.length; i++) {
			String[] parts = tints[i].split(":");
			json.append(i == 0 ? " " : ", ").append(Palettes.KEYS[i]).append(": { color: \"").append(parts[0]).append("\", strength: ").append(parts[1]).append(" }");
		}
		return json.append(" } } } }").toString();
	}

	@Test
	void earlierDefaultTintsAreUpgraded() {
		Palette defaults = SeasonfallConfig.defaults().palettes.get("deciduous");
		SeasonfallConfig grass = parse(deciduous("grass", OLD_DECIDUOUS_GRASS), new Problems());
		assertEquals(defaults.grass.get("mid_winter").strength, grass.palettes.get("deciduous").grass.get("mid_winter").strength);
		SeasonfallConfig foliage = parse(deciduous("foliage", OLD_DECIDUOUS_FOLIAGE), new Problems());
		assertEquals(defaults.foliage.get("mid_spring").strength, foliage.palettes.get("deciduous").foliage.get("mid_spring").strength);
		assertEquals(defaults.foliage.get("mid_winter").color, foliage.palettes.get("deciduous").foliage.get("mid_winter").color);
	}

	@Test
	void editedTintsAreKept() {
		String[] edited = OLD_DECIDUOUS_GRASS.clone();
		edited[0] = "#9DB86A:0.31";
		SeasonfallConfig config = parse(deciduous("grass", edited), new Problems());
		assertEquals(0.35F, config.palettes.get("deciduous").grass.get("mid_autumn").strength);
		assertEquals(0.31F, config.palettes.get("deciduous").grass.get("early_spring").strength);
	}
}
