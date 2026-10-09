package dev.romoslayer.seasonfall.mixin;

import com.google.common.collect.ImmutableMap;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.serialization.Codec;
import dev.romoslayer.seasonfall.climate.BiomeSync;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistrySynchronization;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;

/**
 * The registry data sent to every joining player (in the login packet): biomes are encoded with this season's look.
 * The game sets up which codec encodes each synced registry once, here.
 */
@Mixin(RegistrySynchronization.class)
public abstract class RegistrySynchronizationMixin {
	@SuppressWarnings("unchecked")
	@WrapMethod(method = "put")
	private static void seasonfall$seasonalBiomes(ImmutableMap.Builder<ResourceKey<? extends Registry<?>>, ?> builder,
			ResourceKey<? extends Registry<?>> key, Codec<?> codec, Operation<Void> original) {
		original.call(builder, key, key.equals(Registries.BIOME) ? BiomeSync.joinCodec((Codec<Biome>) codec) : codec);
	}
}
