package dev.romoslayer.seasonfall.network;

import dev.romoslayer.seasonfall.Seasonfall;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Sent only to players who have Seasonfall installed themselves (their game announces the channel; unmodified games
 * never receive it): how biomes look right now, so their game can follow the season without rejoining. Each entry is
 * the full, authoritative look of one biome, its normal look included, so an update can also undo a season. A large
 * biome list is split over several of these.
 * <p>
 * The channel name carries the game and format version: a game with a different one (through a version-translating
 * proxy, say) simply does not announce this one.
 */
public record BiomeSeasonPayload(List<Entry> biomes) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<BiomeSeasonPayload> TYPE = new CustomPacketPayload.Type<>(Seasonfall.id("biome_looks_mc1211_v1"));
	/** Entries per payload: about 30 KB at most, far below any packet limit. */
	public static final int MAX_PAGE_SIZE = 512;

	/**
	 * @param temperature    the temperature the client should use for rain or snow (season included, if any)
	 * @param grass          grass colour, 0xRRGGBB
	 * @param foliage        leaf colour, 0xRRGGBB
	 * @param birch          birch leaf colour, 0xRRGGBB
	 * @param spruce         spruce leaf colour, 0xRRGGBB
	 * @param leafOverlay    colour multiplied over azalea leaves, 0xRRGGBB (white: unchanged)
	 * @param blossomOverlay colour multiplied over cherry and flowering azalea leaves, 0xRRGGBB (white: unchanged)
	 */
	public record Entry(ResourceLocation biome, float temperature, int grass, int foliage, int birch, int spruce, int leafOverlay,
			int blossomOverlay) {
		// More fields than StreamCodec.composite takes on this version, so written out by hand
		static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.of(Entry::write, Entry::read);

		private static void write(RegistryFriendlyByteBuf buf, Entry entry) {
			ResourceLocation.STREAM_CODEC.encode(buf, entry.biome());
			buf.writeFloat(entry.temperature());
			buf.writeInt(entry.grass());
			buf.writeInt(entry.foliage());
			buf.writeInt(entry.birch());
			buf.writeInt(entry.spruce());
			buf.writeInt(entry.leafOverlay());
			buf.writeInt(entry.blossomOverlay());
		}

		private static Entry read(RegistryFriendlyByteBuf buf) {
			return new Entry(ResourceLocation.STREAM_CODEC.decode(buf), buf.readFloat(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(),
					buf.readInt(), buf.readInt());
		}
	}

	public static final StreamCodec<RegistryFriendlyByteBuf, BiomeSeasonPayload> STREAM_CODEC = Entry.STREAM_CODEC
			.apply(ByteBufCodecs.list(MAX_PAGE_SIZE))
			.map(BiomeSeasonPayload::new, BiomeSeasonPayload::biomes);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
