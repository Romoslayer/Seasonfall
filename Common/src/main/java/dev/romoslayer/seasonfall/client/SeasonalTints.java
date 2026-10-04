package dev.romoslayer.seasonfall.client;

import dev.romoslayer.seasonfall.climate.SeasonalBiome;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Leaves the game does not colour by biome: birch and spruce (one fixed colour each) and cherry, azalea and pale oak
 * (whose colour is in their texture). With Seasonfall installed on the client they follow the season too: each biome's
 * colour for them arrives with the live updates, and until one does (or on a server without Seasonfall) they keep the
 * game's own colour.
 */
public final class SeasonalTints {
	private static final int WHITE = -1;

	public static final ColorResolver BIRCH = (biome, x, z) -> pick(seasonal(biome).seasonfall$clientBirch(), FoliageColor.FOLIAGE_BIRCH);
	public static final ColorResolver SPRUCE = (biome, x, z) -> pick(seasonal(biome).seasonfall$clientSpruce(), FoliageColor.FOLIAGE_EVERGREEN);
	/** Laid over leaves whose colour is in their texture; white (no change) until the server says otherwise. */
	public static final ColorResolver LEAF_OVERLAY = (biome, x, z) -> pick(seasonal(biome).seasonfall$clientLeafOverlay(), WHITE);
	/** The gentler overlay for blossoming leaves (cherry, flowering azalea). */
	public static final ColorResolver BLOSSOM_OVERLAY = (biome, x, z) -> pick(seasonal(biome).seasonfall$clientBlossomOverlay(), WHITE);

	private SeasonalTints() {
	}

	/** Used in place of the game's fixed birch or spruce colour; {@code normal} is that colour. */
	public static BlockTintSource leaves(ColorResolver resolver, int normal) {
		return new BlockTintSource() {
			@Override
			public int color(BlockState state) {
				// In the hand and in menus: the usual colour
				return normal;
			}

			@Override
			public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
				return level.getBlockTint(pos, resolver);
			}
		};
	}

	/** For leaves the game does not tint at all: the season's overlay in the world, nothing in the hand. */
	public static BlockTintSource overlay(ColorResolver resolver) {
		return leaves(resolver, WHITE);
	}

	private static int pick(int seasonal, int normal) {
		return seasonal == SeasonalBiome.NO_COLOR ? normal : ARGB.opaque(seasonal);
	}

	private static SeasonalBiome seasonal(Biome biome) {
		return (SeasonalBiome) (Object) biome;
	}
}
