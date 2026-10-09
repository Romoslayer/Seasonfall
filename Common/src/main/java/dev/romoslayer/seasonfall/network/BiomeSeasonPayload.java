package dev.romoslayer.seasonfall.network;

import dev.romoslayer.seasonfall.Seasonfall;
import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
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
public record BiomeSeasonPayload(List<Entry> biomes) {
	public static final ResourceLocation ID = Seasonfall.id("biome_looks_mc1201_v1");
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
		private void write(FriendlyByteBuf buf) {
			buf.writeResourceLocation(this.biome);
			buf.writeFloat(this.temperature);
			buf.writeInt(this.grass);
			buf.writeInt(this.foliage);
			buf.writeInt(this.birch);
			buf.writeInt(this.spruce);
			buf.writeInt(this.leafOverlay);
			buf.writeInt(this.blossomOverlay);
		}

		private static Entry read(FriendlyByteBuf buf) {
			return new Entry(buf.readResourceLocation(), buf.readFloat(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(),
					buf.readInt());
		}
	}

	public void write(FriendlyByteBuf buf) {
		buf.writeVarInt(this.biomes.size());
		for (Entry entry : this.biomes) {
			entry.write(buf);
		}
	}

	public static BiomeSeasonPayload read(FriendlyByteBuf buf) {
		int size = buf.readVarInt();
		if (size < 0 || size > MAX_PAGE_SIZE) {
			throw new DecoderException(size + " biomes in one live update, at most " + MAX_PAGE_SIZE + " expected");
		}
		List<Entry> biomes = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			biomes.add(Entry.read(buf));
		}
		return new BiomeSeasonPayload(List.copyOf(biomes));
	}
}
