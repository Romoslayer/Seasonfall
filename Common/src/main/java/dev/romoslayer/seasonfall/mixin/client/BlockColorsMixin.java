package dev.romoslayer.seasonfall.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.romoslayer.seasonfall.client.SeasonalTints;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Seasonal colours for the leaves the game does not colour by biome. Anything that registers its own colours for these
 * leaves (another mod, say) still takes precedence: it registers after these.
 */
@Mixin(BlockColors.class)
public abstract class BlockColorsMixin {
	/** Birch and spruce: their fixed colours are swapped for seasonal ones where they are registered. */
	@WrapOperation(method = "createDefault",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/color/block/BlockColors;register(Lnet/minecraft/client/color/block/BlockColor;[Lnet/minecraft/world/level/block/Block;)V"))
	private static void seasonfall$seasonalLeaves(BlockColors colors, BlockColor color, Block[] blocks, Operation<Void> original) {
		if (blocks.length == 1 && blocks[0] == Blocks.BIRCH_LEAVES) {
			color = SeasonalTints.leaves(SeasonalTints.BIRCH, FoliageColor.getBirchColor());
		} else if (blocks.length == 1 && blocks[0] == Blocks.SPRUCE_LEAVES) {
			color = SeasonalTints.leaves(SeasonalTints.SPRUCE, FoliageColor.getEvergreenColor());
		}
		original.call(colors, color, blocks);
	}

	/**
	 * Cherry and azalea: no colour of their own, so the season's overlay goes on first, before the game, the loader and
	 * other mods register theirs (and replace it, if they colour these leaves themselves).
	 */
	@ModifyExpressionValue(method = "createDefault", at = @At(value = "NEW", target = "()Lnet/minecraft/client/color/block/BlockColors;"))
	private static BlockColors seasonfall$texturedLeaves(BlockColors colors) {
		colors.register(SeasonalTints.overlay(SeasonalTints.AZALEA_OVERLAY), Blocks.AZALEA_LEAVES, Blocks.FLOWERING_AZALEA_LEAVES);
		// Cherry blossoms are in the same texture: a gentler, warmer overlay keeps them from turning red
		colors.register(SeasonalTints.overlay(SeasonalTints.CHERRY_OVERLAY), Blocks.CHERRY_LEAVES);
		return colors;
	}
}
