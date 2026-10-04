package dev.romoslayer.seasonfall.neoforge;

import dev.romoslayer.seasonfall.Seasonfall;
import dev.romoslayer.seasonfall.client.ClientSeasons;
import dev.romoslayer.seasonfall.network.BiomeSeasonPayload;
import dev.romoslayer.seasonfall.platform.Platform;
import java.nio.file.Path;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(Seasonfall.MOD_ID)
public final class SeasonfallNeoForge implements Platform {
	public SeasonfallNeoForge(IEventBus modBus) {
		modBus.addListener(SeasonfallNeoForge::registerPayloads);
		Seasonfall.init(this);

		IEventBus bus = NeoForge.EVENT_BUS;
		bus.addListener((ServerStartedEvent event) -> Seasonfall.onServerStarted(event.getServer()));
		bus.addListener((ServerStoppingEvent event) -> Seasonfall.onServerStopping(event.getServer()));
		bus.addListener((ServerTickEvent.Post event) -> Seasonfall.onServerTickEnd(event.getServer()));
		bus.addListener((RegisterCommandsEvent event) -> Seasonfall.registerCommands(event.getDispatcher()));
		// No player: data packs were reloaded for everyone (/reload)
		bus.addListener((OnDatapackSyncEvent event) -> {
			if (event.getPlayer() == null) {
				Seasonfall.onDataPackReload(event.getPlayerList().getServer());
			}
		});
	}

	/**
	 * Optional, so that games without Seasonfall (vanilla or NeoForge) can still join; only games that have it are sent
	 * live updates. The handler only ever runs in a game client.
	 */
	private static void registerPayloads(RegisterPayloadHandlersEvent event) {
		event.registrar("1").optional().playToClient(BiomeSeasonPayload.TYPE, BiomeSeasonPayload.STREAM_CODEC,
				(payload, context) -> context.enqueueWork(() -> ClientSeasons.apply(payload)));
	}

	@Override
	public Path configDir() {
		return FMLPaths.CONFIGDIR.get();
	}

	@Override
	public boolean isModLoaded(String modId) {
		return ModList.get().isLoaded(modId);
	}

	@Override
	public boolean canSendLiveUpdates(ServerPlayer player) {
		return player.connection.hasChannel(BiomeSeasonPayload.TYPE);
	}

	@Override
	public void sendLiveUpdate(ServerPlayer player, BiomeSeasonPayload payload) {
		PacketDistributor.sendToPlayer(player, payload);
	}
}
