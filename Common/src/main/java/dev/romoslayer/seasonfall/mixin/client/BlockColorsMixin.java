package dev.romoslayer.seasonfall.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.romoslayer.seasonfall.client.SeasonalTints;
import java.util.List;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.color.block.BlockTintSources;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Seasonal colours for the leaves the game does not colour by biome. Anything that registers its own colours for these
 * leaves (another mod, say) still takes precedence.
 */
@Mixin(BlockColors.class)
public abstract class BlockColorsMixin {
	/** Birch and spruce: their fixed colours are swapped for seasonal ones where they are created. */
	@WrapOperation(method = "createDefault",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/color/block/BlockTintSources;constant(I)Lnet/minecraft/client/color/block/BlockTintSource;"))
	private static BlockTintSource seasonfall$seasonalLeaves(int color, Operation<BlockTintSource> original) {
		if (color == FoliageColor.FOLIAGE_BIRCH) {
			return SeasonalTints.leaves(SeasonalTints.BIRCH, color);
		}
		if (color == FoliageColor.FOLIAGE_EVERGREEN) {
			return SeasonalTints.leaves(SeasonalTints.SPRUCE, color);
		}
		return original.call(color);
	}

	/** Cherry, azalea and pale oak: no colour of their own, so the season's overlay is added if nothing else claimed them. */
	@ModifyReturnValue(method = "createDefault", at = @At("RETURN"))
	private static BlockColors seasonfall$texturedLeaves(BlockColors colors) {
		seasonfall$overlay(colors, SeasonalTints.LEAF_OVERLAY, Blocks.PALE_OAK_LEAVES);
		seasonfall$overlay(colors, SeasonalTints.AZALEA_OVERLAY, Blocks.AZALEA_LEAVES, Blocks.FLOWERING_AZALEA_LEAVES);
		// Cherry blossoms are in the same texture: a gentler, warmer overlay keeps them from turning red
		seasonfall$overlay(colors, SeasonalTints.CHERRY_OVERLAY, Blocks.CHERRY_LEAVES);
		return colors;
	}

	@Unique
	private static void seasonfall$overlay(BlockColors colors, ColorResolver resolver, Block... blocks) {
		for (Block leaves : blocks) {
			if (colors.getTintSources(leaves.defaultBlockState()).isEmpty()) {
				colors.register(List.of(SeasonalTints.overlay(resolver)), leaves);
			}
		}
	}
}
