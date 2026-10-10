package dev.romoslayer.seasonfall.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.CherryParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cherry petals fall with their own texture, which the game never colours. Ones that just fell from cherry leaves (the
 * leaf block is right above where they appear) take the same seasonal colour as the leaves; any others, from commands
 * say, are left alone. Cherry petals are the only falling leaves on this version of the game.
 */
@Mixin(CherryParticle.class)
public abstract class CherryParticleMixin {
	@Inject(method = "<init>", at = @At("TAIL"))
	private void seasonfall$seasonalPetals(ClientLevel level, double x, double y, double z, SpriteSet sprites, CallbackInfo ci) {
		// Cherry leaves drop their petals from just below themselves
		BlockPos above = BlockPos.containing(x, y, z).above();
		BlockState leaves = level.getBlockState(above);
		if (leaves.is(Blocks.CHERRY_LEAVES)) {
			// The leaves' own colour (the season's overlay, or another mod's if it colours them instead)
			int tint = Minecraft.getInstance().getBlockColors().getColor(leaves, level, above, 0);
			if (tint != -1) {
				((Particle) (Object) this).setColor((tint >> 16 & 255) / 255.0F, (tint >> 8 & 255) / 255.0F, (tint & 255) / 255.0F);
			}
		}
	}
}
