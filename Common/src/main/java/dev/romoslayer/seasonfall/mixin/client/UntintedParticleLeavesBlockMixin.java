package dev.romoslayer.seasonfall.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.UntintedParticleLeavesBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Azalea and flowering azalea drop leaves of one fixed green. The season's overlay for the leaves themselves (white
 * when there is none) is laid over that colour too, so what falls matches the tree.
 */
@Mixin(UntintedParticleLeavesBlock.class)
public abstract class UntintedParticleLeavesBlockMixin {
	@WrapOperation(method = "spawnFallingLeavesParticle", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/util/ParticleUtils;spawnParticleBelow(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/particles/ParticleOptions;)V"))
	private void seasonfall$seasonalFallingLeaves(Level level, BlockPos pos, RandomSource random, ParticleOptions particle, Operation<Void> original) {
		if (particle instanceof ColorParticleOption color && level.isClientSide()) {
			int tint = level.getClientLeafTintColor(pos);
			if (tint != -1) {
				particle = ColorParticleOption.create(color.getType(), color.getRed() * ARGB.redFloat(tint), color.getGreen() * ARGB.greenFloat(tint),
						color.getBlue() * ARGB.blueFloat(tint));
			}
		}
		original.call(level, pos, random, particle);
	}
}
