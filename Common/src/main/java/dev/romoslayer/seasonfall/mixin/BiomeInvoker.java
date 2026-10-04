package dev.romoslayer.seasonfall.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** A biome's temperature at a spot, seasonal change included. */
@Mixin(Biome.class)
public interface BiomeInvoker {
	@Invoker("getTemperature")
	float seasonfall$temperature(BlockPos pos, int seaLevel);
}
