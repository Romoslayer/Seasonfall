package dev.romoslayer.seasonfall.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.romoslayer.seasonfall.Seasonfall;
import dev.romoslayer.seasonfall.api.ClimateModifiers;
import dev.romoslayer.seasonfall.api.Season;
import dev.romoslayer.seasonfall.climate.BiomeProfile;
import dev.romoslayer.seasonfall.climate.BiomeSync;
import dev.romoslayer.seasonfall.climate.Climate;
import dev.romoslayer.seasonfall.config.SeasonfallConfig;
import dev.romoslayer.seasonfall.crop.CropGrowth;
import dev.romoslayer.seasonfall.time.DayLength;
import dev.romoslayer.seasonfall.time.SeasonClock;
import dev.romoslayer.seasonfall.time.YearCalendar;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;

/**
 * Administration only (needs the gamemaster permission level):
 * /seasonfall season                         the date and the climate where you stand
 * /seasonfall setseason &lt;season&gt; [percent]   jump to a point in a season (0 = its first day)
 * /seasonfall setday &lt;day&gt;                   jump to a day of the year
 * /seasonfall pause | resume                 stop or restart the year
 * /seasonfall reload                         re-read config/seasonfall.json
 */
public final class SeasonfallCommand {
	private SeasonfallCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		LiteralArgumentBuilder<CommandSourceStack> setSeason = Commands.literal("setseason");
		for (Season season : Season.values()) {
			setSeason.then(Commands.literal(season.id())
					.executes(c -> setSeason(c, season, 0))
					.then(Commands.argument("percent", IntegerArgumentType.integer(0, 99))
							.executes(c -> setSeason(c, season, IntegerArgumentType.getInteger(c, "percent")))));
		}

		dispatcher.register(Commands.literal("seasonfall")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("season").executes(SeasonfallCommand::season))
				.then(setSeason)
				.then(Commands.literal("setday")
						.then(Commands.argument("day", IntegerArgumentType.integer(1)).executes(SeasonfallCommand::setDay)))
				.then(Commands.literal("pause").executes(c -> pause(c, true)))
				.then(Commands.literal("resume").executes(c -> pause(c, false)))
				.then(Commands.literal("reload").executes(SeasonfallCommand::reload)));
	}

	private static int season(CommandContext<CommandSourceStack> context) {
		SeasonClock clock = clock(context);
		if (clock == null) {
			return 0;
		}
		CommandSourceStack source = context.getSource();
		YearCalendar calendar = clock.calendar();
		Season season = calendar.season();
		float progress = calendar.yearProgress();
		send(source, Component.literal(calendar.part() + " " + season.displayName()).withStyle(SeasonClock.color(season), ChatFormatting.BOLD)
				.append(Component.literal(String.format(Locale.ROOT, "  day %d of %d  (day %d of %d, year %d)", calendar.dayOfSeason(),
						calendar.seasonLengthDays(), calendar.dayOfYear(), calendar.yearLengthDays(), calendar.year())).withStyle(ChatFormatting.WHITE)));
		send(source, line(String.format(Locale.ROOT, "Year %.1f%% through, colour step %d of %d, daytime %.2fx normal",
				progress * 100.0F, BiomeSync.stage(progress, SeasonfallConfig.get().visuals.foliageStages) + 1, SeasonfallConfig.get().visuals.foliageStages, DayLength.daytimeMultiplier(progress))));
		if (clock.isPaused()) {
			send(source, Component.literal("The year is paused.").withStyle(ChatFormatting.YELLOW));
		} else if (clock.waitingForPlayers()) {
			send(source, Component.literal("The year waits while nobody is online.").withStyle(ChatFormatting.YELLOW));
		}

		ServerLevel level = source.getLevel();
		BlockPos pos = BlockPos.containing(source.getPosition());
		Holder<Biome> holder = level.getBiome(pos);
		String biomeName = holder.unwrapKey().map(key -> key.identifier().toString()).orElse("this biome");
		BiomeProfile profile = Climate.hasSeasons(level) ? Climate.profile(holder) : null;
		if (profile == null) {
			send(source, line("Here (" + biomeName + "): no seasons."));
			return 1;
		}
		float offset = Climate.offset(holder.value());
		ClimateModifiers climate = Climate.modifiers(holder.value(), progress);
		float wheat = CropGrowth.multiplier(Blocks.WHEAT.defaultBlockState(), level, pos);
		send(source, line(String.format(Locale.ROOT, "Here (%s, %s): temperature %.2f (normally %.2f), wheat grows at %.2fx",
				biomeName, profile.style().id(), profile.baseTemperature() + offset, profile.baseTemperature(), wheat)));
		send(source, line(String.format(Locale.ROOT, "Seasonality: temperature %.2f, leaves %.2f, grass %.2f, crops %.2f, seasonal snow %s",
				profile.temperatureSeasonality(), profile.foliageChange(), profile.grassChange(), profile.cropSeasonality(),
				profile.allowSeasonalSnow() ? "yes" : "no")));
		send(source, line(String.format(Locale.ROOT, "Climate for weather: humidity %.2fx, precipitation %.2fx, storms %.2fx",
				climate.humidityMultiplier(), climate.precipitationMultiplier(), climate.stormProbabilityMultiplier())));
		return 1;
	}

	private static int setSeason(CommandContext<CommandSourceStack> context, Season season, int percent) {
		SeasonClock clock = clock(context);
		if (clock == null) {
			return 0;
		}
		clock.setSeason(season, percent / 100.0F);
		YearCalendar calendar = clock.calendar();
		context.getSource().sendSuccess(() -> Component.literal("Moved the year to " + calendar.part() + " " + season.displayName()
				+ ", day " + calendar.dayOfSeason() + " of " + calendar.seasonLengthDays()), true);
		return 1;
	}

	private static int setDay(CommandContext<CommandSourceStack> context) {
		SeasonClock clock = clock(context);
		if (clock == null) {
			return 0;
		}
		int day = IntegerArgumentType.getInteger(context, "day");
		int length = clock.calendar().yearLengthDays();
		if (day > length) {
			context.getSource().sendFailure(Component.literal("The year only has " + length + " days"));
			return 0;
		}
		clock.setDayOfYear(day);
		YearCalendar calendar = clock.calendar();
		context.getSource().sendSuccess(() -> Component.literal("Moved the year to day " + day + ": " + calendar.part() + " "
				+ calendar.season().displayName()), true);
		return 1;
	}

	private static int pause(CommandContext<CommandSourceStack> context, boolean paused) {
		SeasonClock clock = clock(context);
		if (clock == null) {
			return 0;
		}
		if (clock.isPaused() == paused) {
			context.getSource().sendFailure(Component.literal(paused ? "The year is already paused" : "The year is already running"));
			return 0;
		}
		clock.setPaused(paused);
		context.getSource().sendSuccess(() -> Component.literal(paused ? "Paused the year" : "The year is running again"), true);
		return 1;
	}

	private static int reload(CommandContext<CommandSourceStack> context) {
		String error = SeasonfallConfig.reload();
		if (error != null) {
			context.getSource().sendFailure(Component.literal("Could not reload the config, keeping the old one: " + error));
			return 0;
		}
		SeasonClock clock = Seasonfall.clock();
		if (clock != null) {
			clock.configReloaded();
		}
		context.getSource().sendSuccess(() -> Component.literal("Reloaded the Seasonfall config"), true);
		return 1;
	}

	private static Component line(String text) {
		return Component.literal(text).withStyle(ChatFormatting.GRAY);
	}

	private static void send(CommandSourceStack source, Component message) {
		source.sendSuccess(() -> message, false);
	}

	private static SeasonClock clock(CommandContext<CommandSourceStack> context) {
		SeasonClock clock = Seasonfall.clock();
		if (clock == null) {
			context.getSource().sendFailure(Component.literal("Seasonfall is not running"));
		}
		return clock;
	}
}
