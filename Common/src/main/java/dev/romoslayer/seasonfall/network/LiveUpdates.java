package dev.romoslayer.seasonfall.network;

import dev.romoslayer.seasonfall.Seasonfall;
import dev.romoslayer.seasonfall.climate.BiomeSync;
import dev.romoslayer.seasonfall.config.SeasonfallConfig;
import dev.romoslayer.seasonfall.platform.Platform;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

/**
 * Keeps players who have Seasonfall installed up to date with the season while they play. Whether a player has it is
 * up to their game: it announces the {@link BiomeSeasonPayload} channel, and nothing is ever sent to a game that did not.
 * <ul>
 * <li>Seasons on: every player gets the season's look when the colour step changes, when they join, and whenever the
 * look changes some other way (a command, a config or data pack reload).</li>
 * <li>Seasonfall or its visuals switched off: everyone is sent every biome's normal look once, which undoes whatever
 * season they were showing.</li>
 * <li>Only live updates switched off: nothing more is sent, and players keep what they have until they rejoin.</li>
 * </ul>
 */
public final class LiveUpdates {
	private static final int NEW_PLAYER_CHECK_TICKS = 20;

	private enum Mode { SEASONAL, NORMAL, FROZEN }

	private final Set<UUID> upToDate = new HashSet<>();
	private Mode mode = Mode.SEASONAL;
	private int lastStage = -1;
	private int lastLook;
	private boolean forced;
	private boolean lookMayHaveChanged;
	private int ticksSinceCheck;

	/** Sends everyone an update on the next tick, after the year was moved by a command or the config reloaded. */
	public void forceUpdate() {
		this.forced = true;
	}

	/** Biome profiles were worked out again (a data pack reload, say): check whether that changed what players see. */
	public void profilesRebuilt() {
		this.lookMayHaveChanged = true;
	}

	public void tick(MinecraftServer server, float yearProgress) {
		SeasonfallConfig config = SeasonfallConfig.get();
		Mode wanted = !config.general.enabled || !config.visuals.enabled ? Mode.NORMAL
				: config.visuals.liveUpdatesForModdedClients ? Mode.SEASONAL : Mode.FROZEN;
		boolean modeChanged = wanted != this.mode;
		this.mode = wanted;
		if (wanted == Mode.FROZEN) {
			this.upToDate.clear();
			return;
		}

		int stages = config.visuals.liveUpdateStages;
		int stage = BiomeSync.stage(yearProgress, stages);
		boolean everyone = modeChanged || this.forced || (wanted == Mode.SEASONAL && stage != this.lastStage);
		@Nullable List<BiomeSeasonPayload> payloads = null;
		if (!everyone && this.lookMayHaveChanged) {
			payloads = BiomeSync.livePayloads(server, BiomeSync.stagePhase(yearProgress, stages), wanted == Mode.SEASONAL);
			everyone = payloads.hashCode() != this.lastLook;
		}
		this.lookMayHaveChanged = false;
		if (!everyone && ++this.ticksSinceCheck < NEW_PLAYER_CHECK_TICKS) {
			return;
		}
		this.ticksSinceCheck = 0;
		this.lastStage = stage;
		this.forced = false;
		if (wanted == Mode.NORMAL && !everyone) {
			// Players joining now get normal biomes while joining; only those who saw seasons need undoing
			return;
		}

		Platform platform = Seasonfall.platform();
		Set<UUID> online = new HashSet<>();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			online.add(player.getUUID());
			if ((everyone || !this.upToDate.contains(player.getUUID())) && platform.canSendLiveUpdates(player)) {
				if (payloads == null) {
					payloads = BiomeSync.livePayloads(server, BiomeSync.stagePhase(yearProgress, stages), wanted == Mode.SEASONAL);
				}
				for (BiomeSeasonPayload payload : payloads) {
					platform.sendLiveUpdate(player, payload);
				}
				this.upToDate.add(player.getUUID());
			}
		}
		if (payloads != null) {
			this.lastLook = payloads.hashCode();
		}
		// Forget players who left, so they are sent the season again when they come back
		this.upToDate.retainAll(online);
	}
}
