package dev.romoslayer.seasonfall.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Melting ice the game's own way: water, or nothing where water evaporates. */
@Mixin(IceBlock.class)
public interface IceBlockInvoker {
	@Invoker("melt")
	void seasonfall$melt(BlockState state, Level level, BlockPos pos);
}
