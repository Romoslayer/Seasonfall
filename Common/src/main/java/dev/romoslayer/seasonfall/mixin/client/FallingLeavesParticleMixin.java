package dev.romoslayer.seasonfall.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.FallingLeavesParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Cherry petals and pale oak leaves fall with their own textures, which the game never colours. Ones that just fell from
 * those leaves (the leaf block is right above where they appear) take the same seasonal overlay as the leaves; any
 * others, from commands say, are left alone.
 */
@Mixin({FallingLeavesParticle.CherryProvider.class, FallingLeavesParticle.PaleOakProvider.class})
public abstract class FallingLeavesParticleMixin {
	@ModifyReturnValue(method = "createParticle", at = @At("RETURN"))
	private Particle seasonfall$seasonalFallingLeaves(Particle particle, SimpleParticleType options, ClientLevel level, double x, double y, double z,
			double xAux, double yAux, double zAux, RandomSource random) {
		Block leaves = options == ParticleTypes.CHERRY_LEAVES ? Blocks.CHERRY_LEAVES : Blocks.PALE_OAK_LEAVES;
		BlockPos above = BlockPos.containing(x, y, z).above();
		if (particle instanceof SingleQuadParticle quad && level.getBlockState(above).is(leaves)) {
			int tint = level.getClientLeafTintColor(above);
			quad.setColor(ARGB.redFloat(tint), ARGB.greenFloat(tint), ARGB.blueFloat(tint));
		}
		return particle;
	}
}
