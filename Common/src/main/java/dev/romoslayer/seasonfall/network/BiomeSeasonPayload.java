package dev.romoslayer.seasonfall.network;

import dev.romoslayer.seasonfall.Seasonfall;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Sent only to players who have Seasonfall installed themselves (their game announces the channel; unmodified games
 * never receive it): how biomes look right now, so their game can follow the season without rejoining. Each entry is
 * the full, authoritative look of one biome, its normal look included, so an update can also undo a season. A large
 * biome list is split over several of these.
 * <p>
 * The channel name carries the format version: a game with a different version simply does not announce this one.
 */
public record BiomeSeasonPayload(List<Entry> biomes) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<BiomeSeasonPayload> TYPE = new CustomPacketPayload.Type<>(Seasonfall.id("biome_looks_v5"));
	/** Entries per payload: about 30 KB at most, far below any packet limit. */
	public static final int MAX_PAGE_SIZE = 512;

	/**
	 * @param temperature the temperature the client should use for rain or snow (season included, if any)
	 * @param grass       grass colour, 0xRRGGBB
	 * @param foliage     leaf colour, 0xRRGGBB
	 * @param dryFoliage  leaf litter colour, 0xRRGGBB
	 * @param birch       birch leaf colour, 0xRRGGBB
	 * @param spruce      spruce leaf colour, 0xRRGGBB
	 * @param leafOverlay colour multiplied over azalea and pale oak leaves, 0xRRGGBB (white: unchanged)
	 * @param blossomOverlay colour multiplied over cherry and flowering azalea leaves, 0xRRGGBB (white: unchanged)
	 */
	public record Entry(Identifier biome, float temperature, int grass, int foliage, int dryFoliage, int birch, int spruce, int leafOverlay,
			int blossomOverlay) {
		static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
				Identifier.STREAM_CODEC, Entry::biome,
				ByteBufCodecs.FLOAT, Entry::temperature,
				ByteBufCodecs.INT, Entry::grass,
				ByteBufCodecs.INT, Entry::foliage,
				ByteBufCodecs.INT, Entry::dryFoliage,
				ByteBufCodecs.INT, Entry::birch,
				ByteBufCodecs.INT, Entry::spruce,
				ByteBufCodecs.INT, Entry::leafOverlay,
				ByteBufCodecs.INT, Entry::blossomOverlay,
				Entry::new);
	}

	public static final StreamCodec<RegistryFriendlyByteBuf, BiomeSeasonPayload> STREAM_CODEC = Entry.STREAM_CODEC
			.apply(ByteBufCodecs.list(MAX_PAGE_SIZE))
			.map(BiomeSeasonPayload::new, BiomeSeasonPayload::biomes);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
