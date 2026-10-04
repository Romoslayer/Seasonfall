package dev.romoslayer.seasonfall.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.serialization.DynamicOps;
import dev.romoslayer.seasonfall.climate.BiomeSync;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistrySynchronization;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.repository.KnownPack;
import org.spongepowered.asm.mixin.Mixin;

/** The registry data sent to every joining player: swap in this season's biomes. */
@Mixin(RegistrySynchronization.class)
public abstract class RegistrySynchronizationMixin {
	@WrapMethod(method = "packRegistries")
	private static void seasonfall$seasonalBiomes(DynamicOps<Tag> ops, RegistryAccess registries, Set<KnownPack> clientKnownPacks,
			BiConsumer<ResourceKey<? extends Registry<?>>, List<RegistrySynchronization.PackedRegistryEntry>> output, Operation<Void> original) {
		original.call(ops, registries, clientKnownPacks,
				(BiConsumer<ResourceKey<? extends Registry<?>>, List<RegistrySynchronization.PackedRegistryEntry>>) (key, entries) ->
						output.accept(key, key.equals(Registries.BIOME) ? BiomeSync.rewrite(ops, registries, entries) : entries));
	}
}
