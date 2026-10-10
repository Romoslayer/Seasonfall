package dev.romoslayer.seasonfall.forge;

import dev.romoslayer.seasonfall.Seasonfall;
import dev.romoslayer.seasonfall.client.ClientSeasons;
import dev.romoslayer.seasonfall.network.BiomeSeasonPayload;
import dev.romoslayer.seasonfall.platform.Platform;
import java.nio.file.Path;
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
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** The Forge entrypoint, which NeoForge 47 (still a Forge fork on 1.20.1) loads as well. */
@Mod(Seasonfall.MOD_ID)
public final class SeasonfallForge implements Platform {
	private static final String PROTOCOL = "2";
	/**
	 * Optional on both sides, so that games without Seasonfall (vanilla, Forge or NeoForge) can still join; only games
	 * that have it are sent live updates. The handler only ever runs in a game client.
	 */
	private static final SimpleChannel LIVE_UPDATES = NetworkRegistry.newSimpleChannel(BiomeSeasonPayload.ID, () -> PROTOCOL,
			NetworkRegistry.acceptMissingOr(PROTOCOL), NetworkRegistry.acceptMissingOr(PROTOCOL));

	public SeasonfallForge() {
		LIVE_UPDATES.messageBuilder(BiomeSeasonPayload.class, 0, NetworkDirection.PLAY_TO_CLIENT)
				.encoder(BiomeSeasonPayload::write)
				.decoder(BiomeSeasonPayload::read)
				.consumerMainThread((payload, context) -> ClientSeasons.apply(payload))
				.add();
		Seasonfall.init(this);

		IEventBus bus = MinecraftForge.EVENT_BUS;
		bus.addListener((ServerStartedEvent event) -> Seasonfall.onServerStarted(event.getServer()));
		bus.addListener((ServerStoppingEvent event) -> Seasonfall.onServerStopping(event.getServer()));
		bus.addListener((TickEvent.ServerTickEvent event) -> {
			if (event.phase == TickEvent.Phase.END) {
				Seasonfall.onServerTickEnd(event.getServer());
			}
		});
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
		return LIVE_UPDATES.isRemotePresent(player.connection.connection);
	}

	@Override
	public void sendLiveUpdate(ServerPlayer player, BiomeSeasonPayload payload) {
		LIVE_UPDATES.send(PacketDistributor.PLAYER.with(() -> player), payload);
	}
}
