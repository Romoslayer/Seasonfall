package dev.romoslayer.seasonfall.platform;

import dev.romoslayer.seasonfall.network.BiomeSeasonPayload;
import java.nio.file.Path;
import net.minecraft.server.level.ServerPlayer;

/** The few things the shared code needs from whichever loader is running it. */
public interface Platform {
	/** The folder config files go in. */
	Path configDir();

	boolean isModLoaded(String modId);

	/** Whether this player's game has Seasonfall and can take live updates. */
	boolean canSendLiveUpdates(ServerPlayer player);

	void sendLiveUpdate(ServerPlayer player, BiomeSeasonPayload payload);
}
