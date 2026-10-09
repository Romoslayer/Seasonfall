package dev.romoslayer.seasonfall.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.romoslayer.seasonfall.Seasonfall;
import dev.romoslayer.seasonfall.climate.Climate;
import dev.romoslayer.seasonfall.time.SeasonClock;
import dev.romoslayer.seasonfall.world.VanillaWeather;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
	/**
	 * What falls into cauldrons in a dimension without seasons uses each biome's normal temperature, even where it
	 * shares a biome with a dimension that has them (BiomeMixin does the same for snowfall and freezing).
	 */
	@WrapOperation(method = "tickChunk", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/biome/Biome;getPrecipitationAt(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/biome/Biome$Precipitation;"))
	private Biome.Precipitation seasonfall$precipitation(Biome biome, BlockPos pos, Operation<Biome.Precipitation> original) {
		if (Climate.hasSeasons((ServerLevel) (Object) this)) {
			return original.call(biome, pos);
		}
		return Climate.withoutSeasons(() -> original.call(biome, pos));
	}

	/** Each time the game rolls how long the next dry spell, shower or wait for a storm lasts. */
	@WrapOperation(method = "advanceWeatherCycle",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/util/valueproviders/IntProvider;sample(Lnet/minecraft/util/RandomSource;)I"))
	private int seasonfall$seasonalWeather(IntProvider provider, RandomSource random, Operation<Integer> original) {
		return VanillaWeather.adjust(provider, original.call(provider, random));
	}

	/** The overworld's time of day moving on by a tick, at the speed the season sets for daytime or night. */
	@WrapOperation(method = "tickTime", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setDayTime(J)V"))
	private void seasonfall$dayLength(ServerLevel level, long dayTime, Operation<Void> original) {
		SeasonClock clock = Seasonfall.clock();
		if (clock != null && level.dimension() == Level.OVERWORLD) {
			dayTime = clock.dayLength().advance(level.getDayTime(), dayTime);
		}
		original.call(level, dayTime);
	}
}
