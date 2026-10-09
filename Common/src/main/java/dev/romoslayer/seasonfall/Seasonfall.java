package dev.romoslayer.seasonfall;

import com.mojang.brigadier.CommandDispatcher;
import dev.romoslayer.seasonfall.command.SeasonfallCommand;
import dev.romoslayer.seasonfall.config.SeasonfallConfig;
import dev.romoslayer.seasonfall.platform.Platform;
import dev.romoslayer.seasonfall.time.SeasonClock;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Everything the loader entrypoints call into. Each loader turns its own events into these calls, so the season logic
 * is written once.
 */
public final class Seasonfall {
	public static final String MOD_ID = "seasonfall";
	public static final Logger LOGGER = LoggerFactory.getLogger("Seasonfall");

	private static @Nullable Platform platform;
	private static @Nullable SeasonClock clock;

	private Seasonfall() {
	}

	public static ResourceLocation id(String path) {
		return new ResourceLocation(MOD_ID, path);
	}

	public static void init(Platform loaderPlatform) {
		platform = loaderPlatform;
		SeasonfallConfig.load(loaderPlatform.configDir());
	}

	public static Platform platform() {
		if (platform == null) {
			throw new IllegalStateException("Seasonfall was used before its loader entrypoint ran");
		}
		return platform;
	}

	/** The running world's year, or null while no world is running. */
	public static @Nullable SeasonClock clock() {
		return clock;
	}

	public static void onServerStarted(MinecraftServer server) {
		clock = new SeasonClock(server);
		clock.start();
	}

	public static void onServerStopping(MinecraftServer server) {
		if (clock != null) {
			clock.stop();
			clock = null;
		}
	}

	public static void onServerTickEnd(MinecraftServer server) {
		if (clock != null) {
			clock.tick();
		}
	}

	public static void onDataPackReload(MinecraftServer server) {
		if (clock != null) {
			clock.dataPackReloaded();
		}
	}

	public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
		SeasonfallCommand.register(dispatcher);
	}
}
