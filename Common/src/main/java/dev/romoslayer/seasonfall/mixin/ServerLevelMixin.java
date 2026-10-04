package dev.romoslayer.seasonfall.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.romoslayer.seasonfall.climate.Climate;
import dev.romoslayer.seasonfall.world.VanillaWeather;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
	/**
	 * Snowfall, freezing and what falls into cauldrons in a dimension without seasons use each biome's normal
	 * temperature, even where it shares a biome with a dimension that has them.
	 */
	@WrapMethod(method = "tickPrecipitation")
	private void seasonfall$precipitation(BlockPos pos, Operation<Void> original) {
		if (Climate.hasSeasons((ServerLevel) (Object) this)) {
			original.call(pos);
		} else {
			Climate.withoutSeasons(() -> original.call(pos));
		}
	}

	/** Each time the game rolls how long the next dry spell, shower or wait for a storm lasts. */
	@WrapOperation(method = "advanceWeatherCycle",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/util/valueproviders/IntProvider;sample(Lnet/minecraft/util/RandomSource;)I"))
	private int seasonfall$seasonalWeather(IntProvider provider, RandomSource random, Operation<Integer> original) {
		return VanillaWeather.adjust(provider, original.call(provider, random));
	}
}
