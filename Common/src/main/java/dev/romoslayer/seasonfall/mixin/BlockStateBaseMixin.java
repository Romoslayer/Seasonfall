package dev.romoslayer.seasonfall.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.romoslayer.seasonfall.crop.CropGrowth;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin {
	/** Plants grow on random ticks: the season decides how many of them they get. */
	@WrapMethod(method = "randomTick")
	private void seasonfall$seasonalGrowth(ServerLevel level, BlockPos pos, RandomSource random, Operation<Void> original) {
		CropGrowth.randomTick((BlockState) (Object) this, level, pos, random, () -> original.call(level, pos, random));
	}
}
