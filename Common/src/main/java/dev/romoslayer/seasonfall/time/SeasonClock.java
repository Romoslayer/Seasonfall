package dev.romoslayer.seasonfall.time;

import dev.romoslayer.seasonfall.Seasonfall;
import dev.romoslayer.seasonfall.api.Season;
import dev.romoslayer.seasonfall.climate.Climate;
import dev.romoslayer.seasonfall.config.SeasonfallConfig;
import dev.romoslayer.seasonfall.crop.CropGrowth;
import dev.romoslayer.seasonfall.network.LiveUpdates;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

/**
 * The running server's year: moves it along with the overworld clock, keeps biome temperatures following it, and saves
 * it with the world. The year is one continuous value; nothing changes all at once when a season begins.
 */
public final class SeasonClock {
	private static final int SAVE_INTERVAL_TICKS = 6000;
	private static final long NO_CLOCK = Long.MIN_VALUE;

	private final MinecraftServer server;
	private final Path stateFile;
	private final SeasonState state;
	private final DayLength dayLength = new DayLength();
	private final LiveUpdates liveUpdates = new LiveUpdates();
	private Season season;
	private long lastClockTime = NO_CLOCK;
	private final SeasonHistory history;
	private boolean dirty;
	private boolean saveFailing;
	private float progress;
	private int ticksSinceSave;
	private int ticksSinceUpdate;
	private int ticksSinceProfiles;

	public SeasonClock(MinecraftServer server) {
		this.server = server;
		this.stateFile = server.getWorldPath(LevelResource.ROOT).resolve(Seasonfall.MOD_ID + ".json").normalize();
		SeasonState loaded = SeasonState.load(this.stateFile);
		if (loaded == null) {
			loaded = new SeasonState();
			SeasonfallConfig config = SeasonfallConfig.get();
			loaded.seasonTicks = new YearCalendar(0, config.seasonLength).at(config.startingSeason(), 0.0F);
			this.dirty = true;
		}
		this.state = loaded;
		this.history = new SeasonHistory(loaded.history == null ? List.of() : loaded.history);
		this.progress = this.calendar().yearProgress();
		this.season = this.calendar().season();
	}

	public void start() {
		Climate.start(this.server);
		this.refreshEverything();
		this.save();
		YearCalendar calendar = this.calendar();
		Seasonfall.LOGGER.info("It is {} {}, day {} of {} (year {})", calendar.part(), calendar.season().displayName(), calendar.dayOfSeason(),
				calendar.seasonLengthDays(), calendar.year());
	}

	public void stop() {
		this.save();
		this.dayLength.reset(this.server);
		Climate.stop(this.server);
	}

	public void tick() {
		SeasonfallConfig config = SeasonfallConfig.get();

		long clockTime = this.overworldClockTime();
		if (this.lastClockTime != NO_CLOCK && !this.state.paused && config.general.enabled && config.general.seasonCycleEnabled) {
			long elapsed = clockTime - this.lastClockTime;
			// Time only moves forwards: setting the clock back (/time set) leaves the year where it is
			if (elapsed > 0) {
				this.state.seasonTicks = saturatedAdd(this.state.seasonTicks, elapsed);
				this.dirty = true;
			}
		}
		this.lastClockTime = clockTime;
		this.history.record(this.gameTime(), this.state.seasonTicks);
		this.progress = this.calendar().yearProgress();

		float progress = this.progress;
		if (++this.ticksSinceProfiles >= config.performance.profileRefreshIntervalTicks) {
			// Picks up data pack reloads (biome and block tags)
			this.ticksSinceProfiles = 0;
			this.ticksSinceUpdate = 0;
			Climate.rebuildProfiles(this.server, progress);
			CropGrowth.rebuild(this.server);
			this.liveUpdates.profilesRebuilt();
		} else if (++this.ticksSinceUpdate >= config.performance.seasonUpdateIntervalTicks) {
			this.ticksSinceUpdate = 0;
			Climate.update(this.server, progress);
		}
		this.dayLength.tick(this.server, progress);
		this.liveUpdates.tick(this.server, progress);
		this.checkForNewSeason();

		if (++this.ticksSinceSave >= SAVE_INTERVAL_TICKS) {
			this.save();
		}
	}

	private long overworldClockTime() {
		ServerLevel overworld = this.server.getLevel(Level.OVERWORLD);
		return overworld == null ? 0 : overworld.getOverworldClockTime();
	}

	private void checkForNewSeason() {
		Season now = this.calendar().season();
		if (now == this.season) {
			return;
		}
		this.season = now;
		this.save();
		// Players are not told: they notice the world changing around them
		Seasonfall.LOGGER.info("{} has begun", now.displayName());
	}

	/** Everything that depends on the config or the date, worked out again straight away. */
	private void refreshEverything() {
		this.progress = this.calendar().yearProgress();
		this.ticksSinceProfiles = 0;
		this.ticksSinceUpdate = 0;
		Climate.rebuildProfiles(this.server, this.progress);
		CropGrowth.rebuild(this.server);
		this.season = this.calendar().season();
	}

	// ---- Commands

	public void setSeason(Season season, float progress) {
		this.moveTo(this.calendar().at(season, progress));
	}

	public void setDayOfYear(int day) {
		this.moveTo(this.calendar().atTickOfYear((day - 1) * YearCalendar.TICKS_PER_DAY));
	}

	public void setPaused(boolean paused) {
		// The line of history closes exactly where the pause starts or ends
		this.history.recordJump(this.gameTime(), this.state.seasonTicks, this.state.seasonTicks);
		this.state.paused = paused;
		this.dirty = true;
		this.save();
	}

	public boolean isPaused() {
		return this.state.paused;
	}

	public void configReloaded() {
		this.refreshEverything();
		this.liveUpdates.forceUpdate();
	}

	/** After /reload: tags may have changed which biomes and plants have seasons. */
	public void dataPackReloaded() {
		this.refreshEverything();
		this.liveUpdates.profilesRebuilt();
	}

	private void moveTo(long seasonTicks) {
		long before = this.state.seasonTicks;
		this.state.seasonTicks = Math.max(0, seasonTicks);
		this.history.recordJump(this.gameTime(), before, this.state.seasonTicks);
		this.progress = this.calendar().yearProgress();
		this.dirty = true;
		Climate.update(this.server, this.progress);
		this.liveUpdates.forceUpdate();
		this.checkForNewSeason();
		this.save();
	}

	// ---- Queries

	public YearCalendar calendar() {
		return new YearCalendar(this.state.seasonTicks, SeasonfallConfig.get().seasonLength);
	}

	public long seasonTicks() {
		return this.state.seasonTicks;
	}

	/** How far through the year it is (0 to 1), as of this tick. */
	public float yearProgress() {
		return this.progress;
	}

	/** The overworld's game time: the tick counter that only goes up. */
	public long gameTime() {
		ServerLevel overworld = this.server.getLevel(Level.OVERWORLD);
		return overworld == null ? 0 : overworld.getGameTime();
	}

	/** Where the year was at an earlier game time, following pauses and commands (see {@link SeasonHistory}). */
	public long seasonTicksAt(long gameTime) {
		return this.history.seasonAt(gameTime);
	}

	public Season season() {
		return this.season;
	}

	public static ChatFormatting color(Season season) {
		return switch (season) {
			case SPRING -> ChatFormatting.GREEN;
			case SUMMER -> ChatFormatting.GOLD;
			case AUTUMN -> ChatFormatting.RED;
			case WINTER -> ChatFormatting.AQUA;
		};
	}

	/** Saves if anything changed. A failed save stays pending and is tried again at the next chance. */
	private void save() {
		this.ticksSinceSave = 0;
		if (!this.dirty) {
			return;
		}
		this.state.history = this.history.points();
		if (this.state.save(this.stateFile)) {
			this.dirty = false;
			if (this.saveFailing) {
				this.saveFailing = false;
				Seasonfall.LOGGER.info("Saved the season to {} again", this.stateFile);
			}
		} else if (!this.saveFailing) {
			// Said once per run of failures, not every attempt
			this.saveFailing = true;
			Seasonfall.LOGGER.error("Could not save the season to {}; will keep trying (is the world folder writable?)", this.stateFile);
		}
	}

	static long saturatedAdd(long a, long b) {
		long sum = a + b;
		return ((a ^ sum) & (b ^ sum)) < 0 ? Long.MAX_VALUE : sum;
	}
}
