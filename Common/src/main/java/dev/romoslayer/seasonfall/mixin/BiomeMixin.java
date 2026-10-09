package dev.romoslayer.seasonfall.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.romoslayer.seasonfall.climate.BiomeProfile;
import dev.romoslayer.seasonfall.climate.Climate;
import dev.romoslayer.seasonfall.climate.ColorMaps;
import dev.romoslayer.seasonfall.climate.SeasonalBiome;
import dev.romoslayer.seasonfall.config.SeasonfallConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Biome.class)
public abstract class BiomeMixin implements SeasonalBiome {
	@Unique
	private volatile @Nullable BiomeProfile seasonfall$profile;
	@Unique
	private volatile float seasonfall$temperatureOffset;
	@Unique
	private volatile float seasonfall$clientTemperatureChange;
	@Unique
	private volatile int seasonfall$clientGrass = NO_COLOR;
	@Unique
	private volatile int seasonfall$clientFoliage = NO_COLOR;
	@Unique
	private volatile int seasonfall$clientDryFoliage = NO_COLOR;
	@Unique
	private volatile int seasonfall$clientBirch = NO_COLOR;
	@Unique
	private volatile int seasonfall$clientSpruce = NO_COLOR;
	@Unique
	private volatile int seasonfall$clientLeafOverlay = NO_COLOR;
	@Unique
	private volatile int seasonfall$clientAzaleaOverlay = NO_COLOR;
	@Unique
	private volatile int seasonfall$clientCherryOverlay = NO_COLOR;

	/**
	 * Every temperature check (snowfall, freezing, melting, rain or snow) goes through here. The game caches the value it
	 * computes before this runs, so the cache never holds a temperature from earlier in the year.
	 */
	@ModifyReturnValue(method = "getTemperature(Lnet/minecraft/core/BlockPos;I)F", at = @At("RETURN"))
	private float seasonfall$seasonalTemperature(float temperature, BlockPos pos, int seaLevel) {
		return Climate.adjust((Biome) (Object) this, temperature);
	}

	/**
	 * Freezing and snowfall use the normal temperature where the server's settings say the season should not affect them:
	 * those features switched off, or a dimension without seasons that shares a biome with one that has them. Decided on
	 * the server only; a client's own copy of the config never changes how its world looks.
	 */
	@WrapMethod(method = "shouldFreeze(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Z)Z")
	private boolean seasonfall$freezing(LevelReader level, BlockPos pos, boolean checkNeighbors, Operation<Boolean> original) {
		if (!Climate.isServerThread() || (SeasonfallConfig.get().freezing.seasonalFreezing && seasonfall$seasonal(level))) {
			return original.call(level, pos, checkNeighbors);
		}
		return Climate.withoutSeasons(() -> original.call(level, pos, checkNeighbors));
	}

	@WrapMethod(method = "shouldSnow")
	private boolean seasonfall$snow(LevelReader level, BlockPos pos, Operation<Boolean> original) {
		if (!Climate.isServerThread() || (SeasonfallConfig.get().snow.seasonalSnowPersistence && seasonfall$seasonal(level))) {
			return original.call(level, pos);
		}
		return Climate.withoutSeasons(() -> original.call(level, pos));
	}

	@Unique
	private static boolean seasonfall$seasonal(LevelReader level) {
		return !(level instanceof ServerLevel serverLevel) || Climate.hasSeasons(serverLevel);
	}

	// Colours from a live update replace the biome's own, before the swamp and dark forest grass effects apply as usual

	@ModifyReturnValue(method = "getBaseGrassColor", at = @At("RETURN"))
	private int seasonfall$grass(int color) {
		return this.seasonfall$clientGrass == NO_COLOR ? color : ARGB.opaque(this.seasonfall$clientGrass);
	}

	/** Swamp grass ignores the colour above, so its two colours are moved by the season's change from their average. */
	@ModifyReturnValue(method = "getGrassColor", at = @At("RETURN"))
	private int seasonfall$swampGrass(int color) {
		if (this.seasonfall$clientGrass == NO_COLOR
				|| ((Biome) (Object) this).getSpecialEffects().grassColorModifier() != BiomeSpecialEffects.GrassColorModifier.SWAMP) {
			return color;
		}
		return ARGB.opaque(ColorMaps.shift(color, ColorMaps.SWAMP_GRASS, this.seasonfall$clientGrass));
	}

	@ModifyReturnValue(method = "getFoliageColor", at = @At("RETURN"))
	private int seasonfall$foliage(int color) {
		return this.seasonfall$clientFoliage == NO_COLOR ? color : ARGB.opaque(this.seasonfall$clientFoliage);
	}

	@ModifyReturnValue(method = "getDryFoliageColor", at = @At("RETURN"))
	private int seasonfall$dryFoliage(int color) {
		return this.seasonfall$clientDryFoliage == NO_COLOR ? color : ARGB.opaque(this.seasonfall$clientDryFoliage);
	}

	@Override
	public @Nullable BiomeProfile seasonfall$profile() {
		return this.seasonfall$profile;
	}

	@Override
	public float seasonfall$temperatureOffset() {
		return this.seasonfall$temperatureOffset;
	}

	@Override
	public void seasonfall$setSeason(@Nullable BiomeProfile profile, float temperatureOffset) {
		this.seasonfall$profile = profile;
		this.seasonfall$temperatureOffset = temperatureOffset;
	}

	@Override
	public float seasonfall$clientTemperatureChange() {
		return this.seasonfall$clientTemperatureChange;
	}

	@Override
	public int seasonfall$clientGrass() {
		return this.seasonfall$clientGrass;
	}

	@Override
	public int seasonfall$clientFoliage() {
		return this.seasonfall$clientFoliage;
	}

	@Override
	public int seasonfall$clientDryFoliage() {
		return this.seasonfall$clientDryFoliage;
	}

	@Override
	public int seasonfall$clientBirch() {
		return this.seasonfall$clientBirch;
	}

	@Override
	public int seasonfall$clientSpruce() {
		return this.seasonfall$clientSpruce;
	}

	@Override
	public int seasonfall$clientLeafOverlay() {
		return this.seasonfall$clientLeafOverlay;
	}

	@Override
	public int seasonfall$clientAzaleaOverlay() {
		return this.seasonfall$clientAzaleaOverlay;
	}

	@Override
	public int seasonfall$clientCherryOverlay() {
		return this.seasonfall$clientCherryOverlay;
	}

	@Override
	public void seasonfall$setClientSeason(float temperatureChange, int grass, int foliage, int dryFoliage, int birch, int spruce, int leafOverlay,
			int azaleaOverlay, int cherryOverlay) {
		this.seasonfall$clientAzaleaOverlay = azaleaOverlay;
		this.seasonfall$clientCherryOverlay = cherryOverlay;
		this.seasonfall$clientLeafOverlay = leafOverlay;
		this.seasonfall$clientBirch = birch;
		this.seasonfall$clientSpruce = spruce;
		this.seasonfall$clientTemperatureChange = temperatureChange;
		this.seasonfall$clientGrass = grass;
		this.seasonfall$clientFoliage = foliage;
		this.seasonfall$clientDryFoliage = dryFoliage;
	}
}
