package dev.romoslayer.seasonfall.fabric;

import dev.romoslayer.seasonfall.Seasonfall;
import dev.romoslayer.seasonfall.network.BiomeSeasonPayload;
import dev.romoslayer.seasonfall.platform.Platform;
import java.nio.file.Path;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;

public final class SeasonfallFabric implements ModInitializer, Platform {
	@Override
	public void onInitialize() {
		Seasonfall.init(this);
		// Only games with Seasonfall announce this channel, so only they are ever sent it
		PayloadTypeRegistry.clientboundPlay().register(BiomeSeasonPayload.TYPE, BiomeSeasonPayload.STREAM_CODEC);
		ServerLifecycleEvents.SERVER_STARTED.register(Seasonfall::onServerStarted);
		ServerLifecycleEvents.SERVER_STOPPING.register(Seasonfall::onServerStopping);
		ServerTickEvents.END_SERVER_TICK.register(Seasonfall::onServerTickEnd);
		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> Seasonfall.onDataPackReload(server));
		CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> Seasonfall.registerCommands(dispatcher));
	}

	@Override
	public Path configDir() {
		return FabricLoader.getInstance().getConfigDir();
	}

	@Override
	public boolean isModLoaded(String modId) {
		return FabricLoader.getInstance().isModLoaded(modId);
	}

	@Override
	public boolean canSendLiveUpdates(ServerPlayer player) {
		return ServerPlayNetworking.canSend(player, BiomeSeasonPayload.TYPE);
	}

	@Override
	public void sendLiveUpdate(ServerPlayer player, BiomeSeasonPayload payload) {
		ServerPlayNetworking.send(player, payload);
	}
}
