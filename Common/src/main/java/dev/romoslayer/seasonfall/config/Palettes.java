package dev.romoslayer.seasonfall.config;

import dev.romoslayer.seasonfall.climate.BiomeStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/** The default seasonal palettes, one per biome style. */
final class Palettes {
	/** Palette keys, each sitting in the middle of its third of a season. */
	static final String[] KEYS = {
			"early_spring", "mid_spring", "late_spring",
			"early_summer", "mid_summer", "late_summer",
			"early_autumn", "mid_autumn", "late_autumn",
			"early_winter", "mid_winter", "late_winter"
	};

	private Palettes() {
	}

	static Map<String, Palette> defaults() {
		Map<String, Palette> palettes = new LinkedHashMap<>();
		// Broadleaf woods: pale new leaves in spring, deep summer green, then yellow, orange, orange-brown and a grey winter
		palettes.put(BiomeStyle.DECIDUOUS.id(), new Palette(
				tints("#C4D878:0.6", "#B4E060:0.65", "#98D450:0.5", "#5AA834:0.15", "#3F8A2A:0.2", "#8FA436:0.2",
						"#E0C828:0.55", "#FF8A10:0.8", "#E2502A:0.82", "#8E7866:0.72", "#8A8A8A:0.85", "#8C8E86:0.75"),
				// Grass: bright new growth in spring, deep green in summer, olive to tawny in autumn, dormant grey in winter
				tints("#A9C468:0.45", "#8FD04E:0.45", "#74C83E:0.35", "#5DB038:0.2", "#5A9E36:0.25", "#8EA040:0.3",
						"#A8A044:0.45", "#B08A44:0.55", "#9A7848:0.62", "#928878:0.72", "#949490:0.8", "#92948A:0.7")));
		// Open grassland: the grass does most of the changing, drying to straw in late summer and autumn
		palettes.put(BiomeStyle.TEMPERATE.id(), new Palette(
				tints("#C4D878:0.55", "#B4E060:0.6", "#98D450:0.45", "#4A9E2E:0.1", "#5E9A30:0.15", "#9CA83A:0.2",
						"#D4C030:0.45", "#EC981E:0.62", "#C86A2C:0.66", "#8E7866:0.68", "#8A8A8A:0.8", "#8C8E86:0.7"),
				tints("#A6C468:0.45", "#8CD24C:0.45", "#74C83E:0.35", "#6CB83C:0.2", "#8FB048:0.25", "#C2B456:0.4",
						"#C8AE58:0.5", "#BC9A52:0.6", "#A88A5A:0.65", "#9C9282:0.75", "#9A9A94:0.82", "#9A9E90:0.72")));
		// Conifers keep their needles: only a little brighter in spring and darker, greyer in winter
		palettes.put(BiomeStyle.EVERGREEN.id(), new Palette(
				tints("#88B868:0.3", "#78BC58:0.25", "#62AE4C:0.15", "#447F3C:0.1", "#3E7A3A:0.15", "#4A7A3C:0.1",
						"#58744A:0.15", "#557050:0.2", "#4E6A58:0.25", "#5C6C66:0.4", "#646C6C:0.5", "#606C66:0.42"),
				tints("#9AB876:0.4", "#80C45A:0.4", "#6EBA4C:0.3", "#62A646:0.15", "#6A9E48:0.2", "#8A9C52:0.3",
						"#9E9856:0.45", "#9A8A5C:0.55", "#8E8468:0.6", "#8E8E86:0.66", "#969A9C:0.72", "#8E9690:0.6")));
		// Swamps: olive and brown rather than bright autumn colours
		palettes.put(BiomeStyle.WETLAND.id(), new Palette(
				tints("#ACCA6A:0.5", "#A0D05A:0.5", "#86C64A:0.4", "#4E8E36:0.1", "#4A8432:0.15", "#7A8E38:0.2",
						"#9A9A3A:0.3", "#A08A36:0.4", "#8A7038:0.45", "#7A7264:0.6", "#828280:0.7", "#848A7E:0.6"),
				tints("#92AA6A:0.4", "#7CB652:0.4", "#68AA46:0.3", "#5E9A3E:0.15", "#7A9A44:0.2", "#9A9C4C:0.3",
						"#A09A4E:0.45", "#9C8A4C:0.55", "#8C7C52:0.6", "#88847A:0.64", "#8E928E:0.7", "#868C80:0.6")));
		// Savanna: greens up with the spring rains, then bakes dry
		palettes.put(BiomeStyle.SAVANNA.id(), new Palette(
				tints("#8CB84A:0.25", "#84B848:0.2", "#9AB044:0.15", "#B0A846:0.2", "#BCA44A:0.3", "#BAA04C:0.3",
						"#B09C4E:0.25", "#A89A50:0.2", "#A49A58:0.2", "#9E9A60:0.2", "#9A9A62:0.2", "#90A856:0.2"),
				tints("#94B850:0.35", "#88BC4C:0.4", "#A6B04C:0.3", "#BCAA50:0.3", "#C8A854:0.45", "#C4A456:0.45",
						"#B8A258:0.35", "#AE9E5A:0.3", "#A89C60:0.25", "#A29C66:0.25", "#9E9C68:0.25", "#98AC5C:0.3")));
		// Deserts and badlands: barely any change
		palettes.put(BiomeStyle.ARID.id(), new Palette(
				tints("#9AB050:0.1", "#A0AE50:0.1", "#ACA850:0.1", "#BCA854:0.15", "#C6A858:0.2", "#C0A65A:0.15",
						"#B4A45C:0.1", "#AEA25E:0.1", "#A8A060:0.1", "#A4A064:0.1", "#A0A066:0.1", "#9CAA5A:0.1"),
				tints("#9CB454:0.1", "#A2B254:0.1", "#AEAC54:0.1", "#C0AC58:0.15", "#CCAC5C:0.2", "#C6AA5E:0.15",
						"#BAA860:0.1", "#B2A662:0.1", "#ACA464:0.1", "#A8A468:0.1", "#A4A46A:0.1", "#A0AE5E:0.1")));
		// Jungles stay green: a touch lusher in the wet half of the year
		palettes.put(BiomeStyle.TROPICAL.id(), new Palette(
				tints("#2EA82E:0.1", "#28AE28:0.1", "#22B022:0.12", "#1EAE1E:0.12", "#22A822:0.1", "#38A42E:0.08",
						"#4A9E34:0.08", "#5A9A3A:0.1", "#5E983C:0.1", "#5A9A3A:0.1", "#4E9E36:0.08", "#3CA430:0.08"),
				tints("#3EAC36:0.1", "#38B032:0.1", "#32B22E:0.12", "#2EB02C:0.12", "#34AA30:0.1", "#48A63A:0.08",
						"#58A23E:0.08", "#669E44:0.1", "#6A9C46:0.1", "#669E44:0.1", "#5AA240:0.08", "#4AA83A:0.08")));
		// Snowy lands: a short green summer and a long grey-blue winter
		palettes.put(BiomeStyle.FROZEN.id(), new Palette(
				tints("#8CAA88:0.25", "#82B07A:0.25", "#72AE6C:0.2", "#64A062:0.15", "#6A9C62:0.15", "#7A9666:0.2",
						"#86907A:0.3", "#8A9488:0.35", "#8E9A98:0.4", "#929E9E:0.5", "#96A2A6:0.55", "#8E9C9A:0.42"),
				tints("#8AA290:0.3", "#80A880:0.3", "#76AE70:0.3", "#6EAA66:0.25", "#74A468:0.2", "#8A9E6C:0.3",
						"#9A9A7C:0.4", "#9A9C8E:0.45", "#9EA4A0:0.5", "#A0AAAC:0.6", "#A2ACB2:0.62", "#98A4A2:0.5")));
		return palettes;
	}

	/**
	 * The default tints of earlier versions (too faint grass at first, then less light spring leaves and a less grey
	 * winter). A config file that still has one of these exactly, written out unchanged by an earlier version, gets the
	 * current default instead; edited ones are kept.
	 */
	private static final Map<String, List<String[]>> PREVIOUS_FOLIAGE = Map.of(
			BiomeStyle.DECIDUOUS.id(), List.<String[]>of(new String[] {"#A7C46A:0.35", "#8CCB4E:0.25", "#6CC23A:0.2", "#4A9E2E:0.15", "#3F8A2A:0.2",
					"#8FA436:0.2", "#E0C828:0.55", "#FF8A10:0.8", "#E2502A:0.82", "#A0703F:0.65", "#7C7666:0.55", "#87866A:0.5"}),
			BiomeStyle.TEMPERATE.id(), List.<String[]>of(new String[] {"#A7C46A:0.3", "#8CCB4E:0.2", "#6CC23A:0.15", "#4A9E2E:0.1", "#5E9A30:0.15",
					"#9CA83A:0.2", "#D4C030:0.45", "#EC981E:0.62", "#C86A2C:0.66", "#967046:0.55", "#7C7666:0.5", "#87866A:0.45"}),
			BiomeStyle.EVERGREEN.id(), List.<String[]>of(new String[] {"#6F9E58:0.2", "#62A84E:0.15", "#55A046:0.1", "#447F3C:0.1", "#3E7A3A:0.15",
					"#4A7A3C:0.1", "#58744A:0.15", "#557050:0.2", "#4E6A58:0.25", "#4A6660:0.3", "#4A6466:0.35", "#52685E:0.3"}),
			BiomeStyle.WETLAND.id(), List.<String[]>of(new String[] {"#8AA85A:0.25", "#76B04A:0.2", "#62A63E:0.15", "#4E8E36:0.1", "#4A8432:0.15",
					"#7A8E38:0.2", "#9A9A3A:0.3", "#A08A36:0.4", "#8A7038:0.45", "#706448:0.45", "#6C6A5A:0.45", "#747658:0.4"}),
			BiomeStyle.FROZEN.id(), List.<String[]>of(new String[] {"#7E9A84:0.2", "#76A276:0.2", "#6CA46A:0.2", "#64A062:0.15", "#6A9C62:0.15",
					"#7A9666:0.2", "#86907A:0.3", "#8A9488:0.35", "#8E9A98:0.4", "#90A0A4:0.4", "#92A2A8:0.4", "#8A9C98:0.3"}));

	private static final Map<String, List<String[]>> PREVIOUS_GRASS = Map.of(
			BiomeStyle.DECIDUOUS.id(), List.of(
					new String[] {"#9DB86A:0.3", "#7FC44E:0.2", "#6CC23A:0.15", "#5AA83A:0.1", "#6E9E3A:0.15", "#A5A548:0.2",
							"#B4A64C:0.25", "#B08E48:0.35", "#9A7E50:0.45", "#8E8670:0.45", "#9AA39A:0.5", "#8F9A78:0.4"},
					new String[] {"#A9C468:0.45", "#8FD04E:0.45", "#74C83E:0.35", "#5DB038:0.2", "#5A9E36:0.25", "#8EA040:0.3",
							"#A8A044:0.45", "#B08A44:0.55", "#9A7848:0.62", "#94866A:0.65", "#9A9076:0.68", "#949474:0.58"}),
			BiomeStyle.TEMPERATE.id(), List.of(
					new String[] {"#A3BC6C:0.3", "#84C650:0.2", "#6CC23A:0.15", "#62AA3C:0.1", "#9BA848:0.2", "#BFB050:0.3",
							"#C2A850:0.35", "#B8964A:0.45", "#A08552:0.5", "#958C72:0.5", "#A0A69C:0.55", "#949C7C:0.45"},
					new String[] {"#A6C468:0.45", "#8CD24C:0.45", "#74C83E:0.35", "#6CB83C:0.2", "#8FB048:0.25", "#C2B456:0.4",
							"#C8AE58:0.5", "#BC9A52:0.6", "#A88A5A:0.65", "#9C8E6C:0.7", "#9A9478:0.7", "#9AA07A:0.6"}),
			BiomeStyle.EVERGREEN.id(), List.of(
					new String[] {"#8FB070:0.3", "#7CBC56:0.2", "#6CB646:0.15", "#5EA040:0.1", "#6A9A44:0.15", "#8A9C4E:0.2",
							"#9C9852:0.3", "#988A58:0.4", "#8C8466:0.45", "#8E8C80:0.5", "#9AA0A0:0.55", "#8E9888:0.45"},
					new String[] {"#9AB876:0.4", "#80C45A:0.4", "#6EBA4C:0.3", "#62A646:0.15", "#6A9E48:0.2", "#8A9C52:0.3",
							"#9E9856:0.45", "#9A8A5C:0.55", "#8E8468:0.6", "#8C8A7C:0.62", "#969C9C:0.62", "#8E9888:0.52"}),
			BiomeStyle.WETLAND.id(), List.of(
					new String[] {"#8EA866:0.25", "#7AB250:0.2", "#68A844:0.15", "#5E9A3E:0.1", "#7A9A44:0.15", "#9A9C4C:0.2",
							"#A09A4E:0.25", "#9C8A4C:0.35", "#8C7C52:0.4", "#86806C:0.4", "#8E9488:0.45", "#868E74:0.35"},
					new String[] {"#92AA6A:0.4", "#7CB652:0.4", "#68AA46:0.3", "#5E9A3E:0.15", "#7A9A44:0.2", "#9A9C4C:0.3",
							"#A09A4E:0.45", "#9C8A4C:0.55", "#8C7C52:0.6", "#86806C:0.6", "#8E9488:0.6", "#868E74:0.5"}),
			BiomeStyle.SAVANNA.id(), List.<String[]>of(
					new String[] {"#94B850:0.25", "#8CB84C:0.2", "#A6B04C:0.2", "#BCAA50:0.25", "#C8A854:0.35", "#C4A456:0.35",
							"#B8A258:0.3", "#AE9E5A:0.25", "#A89C60:0.2", "#A29C66:0.2", "#9E9C68:0.2", "#98AC5C:0.2"}),
			BiomeStyle.FROZEN.id(), List.of(
					new String[] {"#8AA290:0.2", "#80A880:0.2", "#76AA74:0.2", "#6EA66C:0.15", "#74A26C:0.15", "#849C70:0.2",
							"#929882:0.3", "#969C90:0.35", "#9AA2A0:0.4", "#9EA8AC:0.4", "#A0AAB0:0.4", "#96A2A0:0.3"},
					new String[] {"#8AA290:0.3", "#80A880:0.3", "#76AE70:0.3", "#6EAA66:0.25", "#74A468:0.2", "#8A9E6C:0.3",
							"#9A9A7C:0.4", "#9A9C8E:0.45", "#9EA4A0:0.5", "#A0AAAC:0.5", "#A2ACB2:0.5", "#98A4A2:0.4"}));

	/** The configured tints, or the default ones when the configured ones are an earlier version's defaults. */
	private static Map<String, Palette.Tint> upgraded(@Nullable Map<String, Palette.Tint> tints, @Nullable List<String[]> previous,
			Map<String, Palette.Tint> fallback) {
		if (tints != null && previous != null) {
			for (String[] version : previous) {
				if (matches(tints, tints(version))) {
					return fallback;
				}
			}
		}
		return completeTints(tints, fallback);
	}

	private static boolean matches(Map<String, Palette.Tint> tints, Map<String, Palette.Tint> expected) {
		if (tints.size() != KEYS.length) {
			return false;
		}
		for (String key : KEYS) {
			Palette.Tint tint = tints.get(key);
			Palette.Tint old = expected.get(key);
			if (tint == null || tint.color == null || !tint.color.equalsIgnoreCase(old.color) || Math.abs(tint.strength - old.strength) > 1.0E-4F) {
				return false;
			}
		}
		return true;
	}

	/** Every style gets a palette and every palette all twelve tints; anything broken falls back to the default. */
	static Map<String, Palette> complete(@Nullable Map<String, Palette> palettes) {
		Map<String, Palette> defaults = defaults();
		Map<String, Palette> result = new LinkedHashMap<>();
		for (Map.Entry<String, Palette> entry : defaults.entrySet()) {
			Palette configured = palettes == null ? null : palettes.get(entry.getKey());
			Palette fallback = entry.getValue();
			result.put(entry.getKey(), configured == null ? fallback
					: new Palette(upgraded(configured.foliage, PREVIOUS_FOLIAGE.get(entry.getKey()), fallback.foliage),
							upgraded(configured.grass, PREVIOUS_GRASS.get(entry.getKey()), fallback.grass)));
		}
		if (palettes != null) {
			// Palettes for styles of the user's own naming are kept, though only the built-in styles use them
			palettes.forEach((name, palette) -> {
				if (!result.containsKey(name) && palette != null) {
					Palette fallback = defaults.get(BiomeStyle.TEMPERATE.id());
					result.put(name, new Palette(completeTints(palette.foliage, fallback.foliage), completeTints(palette.grass, fallback.grass)));
				}
			});
		}
		return result;
	}

	private static Map<String, Palette.Tint> completeTints(@Nullable Map<String, Palette.Tint> tints, Map<String, Palette.Tint> fallback) {
		Map<String, Palette.Tint> result = new LinkedHashMap<>();
		for (String key : KEYS) {
			Palette.Tint tint = tints == null ? null : tints.get(key);
			if (tint == null || !tint.isValid()) {
				tint = fallback.get(key);
			}
			tint.strength = SeasonfallConfig.clamp01(tint.strength);
			result.put(key, tint);
		}
		return result;
	}

	private static Map<String, Palette.Tint> tints(String... values) {
		Map<String, Palette.Tint> tints = new LinkedHashMap<>();
		for (int i = 0; i < KEYS.length; i++) {
			String[] parts = values[i].split(":");
			tints.put(KEYS[i], new Palette.Tint(parts[0], Float.parseFloat(parts[1])));
		}
		return tints;
	}
}
