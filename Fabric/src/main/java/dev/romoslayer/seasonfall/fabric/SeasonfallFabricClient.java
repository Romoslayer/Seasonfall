package dev.romoslayer.seasonfall.fabric;

import dev.romoslayer.seasonfall.client.ClientSeasons;
import dev.romoslayer.seasonfall.network.BiomeSeasonPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Optional: with Seasonfall installed on the client too, the season updates while playing instead of on rejoin. */
public final class SeasonfallFabricClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Read on the network thread, applied on the game thread
		ClientPlayNetworking.registerGlobalReceiver(BiomeSeasonPayload.ID, (client, handler, buf, sender) -> {
			BiomeSeasonPayload payload = BiomeSeasonPayload.read(buf);
			client.execute(() -> ClientSeasons.apply(payload));
		});
	}
}
