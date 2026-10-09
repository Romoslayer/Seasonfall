package dev.romoslayer.seasonfall.world;

import dev.romoslayer.seasonfall.Seasonfall;
import dev.romoslayer.seasonfall.climate.Climate;
import dev.romoslayer.seasonfall.config.SeasonfallConfig;
import dev.romoslayer.seasonfall.mixin.BiomeInvoker;
import dev.romoslayer.seasonfall.mixin.IceBlockInvoker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * The thaw. Snow and ice come only from the game itself: rain falls as snow, and still water freezes, wherever the
 * season has made a biome cold enough. Once it warms past the thaw temperature they melt on the same random ticks the
 * game melts them on next to a torch, so the snow goes gradually rather than all at once.
 * <p>
 * Only snow and ice open to the sky are touched, so anything built indoors stays as it is.
 */
public final class SnowAndIce {
	private SnowAndIce() {
	}

	/** After a snow layer's own random tick, if it is still there. */
	public static void snowTick(BlockState state, ServerLevel level, BlockPos pos) {
		SeasonfallConfig config = SeasonfallConfig.get();
		if (!config.snow.seasonalSnowPersistence || !isOpenToSky(level, pos) || !isThawing(level, pos)) {
			return;
		}
		// A layer at a time, without dropping snowballs everywhere
		int layers = state.getValue(SnowLayerBlock.LAYERS);
		if (layers > 1) {
			level.setBlockAndUpdate(pos, state.setValue(SnowLayerBlock.LAYERS, layers - 1));
		} else {
			level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
		}
	}

	/** After an ice block's own random tick, if it is still there. */
	public static void iceTick(BlockState state, ServerLevel level, BlockPos pos) {
		SeasonfallConfig config = SeasonfallConfig.get();
		if (!config.freezing.seasonalFreezing || !state.is(Blocks.ICE) || !isOpenToSky(level, pos) || !isThawing(level, pos)) {
			return;
		}
		if (state.getBlock() instanceof IceBlock ice) {
			// Back to water the game's own way (or nothing, where water evaporates)
			((IceBlockInvoker) ice).seasonfall$melt(state, level, pos);
		}
	}

	private static boolean isThawing(ServerLevel level, BlockPos pos) {
		if (Seasonfall.clock() == null || !Climate.hasSeasons(level)) {
			return false;
		}
		Holder<Biome> biome = level.getBiome(pos);
		if (Climate.profile(biome) == null) {
			return false;
		}
		float temperature = ((BiomeInvoker) (Object) biome.value()).seasonfall$temperature(pos);
		return temperature >= SeasonfallConfig.get().freezing.thawTemperatureThreshold;
	}

	/** Nothing above but air or other things that do not stop rain: the same place snow would settle or water freeze. */
	private static boolean isOpenToSky(ServerLevel level, BlockPos pos) {
		return pos.getY() + 1 >= level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
	}
}
