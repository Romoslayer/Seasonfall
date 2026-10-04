package dev.romoslayer.seasonfall.mixin.client;

import dev.romoslayer.seasonfall.client.SeasonalTints;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.client.color.block.BlockTintCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ColorResolver;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Gives the client's per-biome colour caches an entry for seasonal birch and spruce leaves. */
@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
	@Shadow
	@Final
	private Object2ObjectArrayMap<ColorResolver, BlockTintCache> tintCaches;

	@Shadow
	public abstract int calculateBlockTint(BlockPos pos, ColorResolver resolver);

	@Inject(method = "<init>", at = @At("TAIL"))
	private void seasonfall$leafCaches(CallbackInfo ci) {
		this.tintCaches.put(SeasonalTints.BIRCH, new BlockTintCache(pos -> this.calculateBlockTint(pos, SeasonalTints.BIRCH)));
		this.tintCaches.put(SeasonalTints.SPRUCE, new BlockTintCache(pos -> this.calculateBlockTint(pos, SeasonalTints.SPRUCE)));
		this.tintCaches.put(SeasonalTints.LEAF_OVERLAY, new BlockTintCache(pos -> this.calculateBlockTint(pos, SeasonalTints.LEAF_OVERLAY)));
		this.tintCaches.put(SeasonalTints.BLOSSOM_OVERLAY, new BlockTintCache(pos -> this.calculateBlockTint(pos, SeasonalTints.BLOSSOM_OVERLAY)));
	}
}
