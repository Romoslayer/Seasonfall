package dev.romoslayer.seasonfall.mixin;

import dev.romoslayer.seasonfall.world.SnowAndIce;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SnowLayerBlock.class)
public abstract class SnowLayerBlockMixin {
	/** Snow melts on the random ticks the game already gives it, once the season has warmed up. */
	@Inject(method = "randomTick", at = @At("TAIL"))
	private void seasonfall$thaw(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
		BlockState now = level.getBlockState(pos);
		if (now.getBlock() == (Object) this) {
			SnowAndIce.snowTick(now, level, pos);
		}
	}
}
