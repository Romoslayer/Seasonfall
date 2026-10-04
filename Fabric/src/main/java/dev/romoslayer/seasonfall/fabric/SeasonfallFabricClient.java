package dev.romoslayer.seasonfall.fabric;

import dev.romoslayer.seasonfall.client.ClientSeasons;
import dev.romoslayer.seasonfall.network.BiomeSeasonPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Optional: with Seasonfall installed on the client too, the season updates while playing instead of on rejoin. */
public final class SeasonfallFabricClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(BiomeSeasonPayload.TYPE, (payload, context) -> ClientSeasons.apply(payload));
	}
}
