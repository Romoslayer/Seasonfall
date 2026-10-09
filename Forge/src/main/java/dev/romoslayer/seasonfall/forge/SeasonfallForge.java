package dev.romoslayer.seasonfall.forge;

import dev.romoslayer.seasonfall.Seasonfall;
import dev.romoslayer.seasonfall.client.ClientSeasons;
import dev.romoslayer.seasonfall.network.BiomeSeasonPayload;
import dev.romoslayer.seasonfall.platform.Platform;
import java.nio.file.Path;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;

@Mod(Seasonfall.MOD_ID)
public final class SeasonfallForge implements Platform {
	/**
	 * Optional, so that games without Seasonfall (vanilla or Forge) can still join; only games that have it are sent
	 * live updates. The handler only ever runs in a game client.
	 */
	private static final Channel<CustomPacketPayload> LIVE_UPDATES = ChannelBuilder.named(BiomeSeasonPayload.TYPE.id())
			.optional()
			.payloadChannel()
			.play()
			.clientbound()
			.addMain(BiomeSeasonPayload.TYPE, BiomeSeasonPayload.STREAM_CODEC, (payload, context) -> ClientSeasons.apply(payload))
			.build();

	public SeasonfallForge() {
		Seasonfall.init(this);

		IEventBus bus = MinecraftForge.EVENT_BUS;
		bus.addListener((ServerStartedEvent event) -> Seasonfall.onServerStarted(event.getServer()));
		bus.addListener((ServerStoppingEvent event) -> Seasonfall.onServerStopping(event.getServer()));
		bus.addListener((TickEvent.ServerTickEvent.Post event) -> Seasonfall.onServerTickEnd(event.getServer()));
		bus.addListener((RegisterCommandsEvent event) -> Seasonfall.registerCommands(event.getDispatcher()));
		// No player: data packs were reloaded for everyone (/reload)
		bus.addListener((OnDatapackSyncEvent event) -> {
			if (event.getPlayer() == null) {
				Seasonfall.onDataPackReload(event.getPlayerList().getServer());
			}
		});
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
		return LIVE_UPDATES.isRemotePresent(player.connection.getConnection());
	}

	@Override
	public void sendLiveUpdate(ServerPlayer player, BiomeSeasonPayload payload) {
		LIVE_UPDATES.send(payload, PacketDistributor.PLAYER.with(player));
	}
}
