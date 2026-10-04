package dev.romoslayer.seasonfall.time;

import java.util.ArrayList;
import java.util.List;

/**
 * How the year actually moved against game time: points of (game time, season time) with straight lines between them.
 * Pauses are flat stretches, commands that move the year are two points at the same game time, and normal play is one
 * long line, so a few points cover a long time. Used to work out which seasons a chunk really went through while nobody
 * was near it.
 * <p>
 * Game time is the world's tick counter, which only goes up (unlike the time of day, which commands and sleeping move).
 * Before the first point the year is assumed to have run at its normal pace.
 * <p>
 * The year is sampled every {@link #SPACING} game ticks. Samples join the current straight line for as long as none of
 * them strays more than {@link #TOLERANCE} season ticks from it; then the line is closed and a new one starts.
 */
public final class SeasonHistory {
	/** Game ticks between samples. */
	static final long SPACING = 1200L;
	/** How far (in season ticks) a sample may stray from the line it is part of: a tenth of a day. */
	static final long TOLERANCE = 2400L;
	/** Samples a single line may hold before it is closed anyway. */
	static final int MAX_LINE_SAMPLES = 256;
	/** Oldest points are dropped past this many. */
	static final int MAX_POINTS = 2048;

	/** Closed points, oldest first. Game times never go down. */
	private final List<long[]> points = new ArrayList<>();
	/** Samples since the last closed point, on the current line. */
	private final List<long[]> line = new ArrayList<>();
	/** The latest position, which may be newer than the last sample. */
	private long[] live;
	/** Every point, worked out once per change for lookups. */
	private List<long[]> snapshot;

	public SeasonHistory() {
	}

	/** Restores saved points, dropping anything malformed or out of order. */
	public SeasonHistory(List<long[]> saved) {
		long lastGame = Long.MIN_VALUE;
		for (long[] point : saved) {
			if (point != null && point.length == 2 && point[0] >= lastGame) {
				this.points.add(new long[]{point[0], point[1]});
				lastGame = point[0];
			}
		}
	}

	/** Every point, for saving. */
	public List<long[]> points() {
		List<long[]> all = new ArrayList<>(this.points.size() + 2);
		for (long[] point : this.points) {
			all.add(point.clone());
		}
		if (!this.line.isEmpty()) {
			all.add(this.line.getLast().clone());
		}
		if (this.live != null && (all.isEmpty() || !same(all.getLast(), this.live))) {
			all.add(this.live.clone());
		}
		return all;
	}

	/** Notes where the year is at this game time. Cheap enough to call every tick. */
	public void record(long gameTime, long seasonTicks) {
		this.snapshot = null;
		long[] newest = this.newest();
		if (newest != null && gameTime < newest[0]) {
			// Game time never goes back; a world restored from an older save starts the history again
			this.points.clear();
			this.line.clear();
			newest = null;
		}
		this.live = new long[]{gameTime, seasonTicks};
		if (this.points.isEmpty()) {
			this.points.add(this.live.clone());
			return;
		}
		long[] lastSample = this.line.isEmpty() ? this.points.getLast() : this.line.getLast();
		if (gameTime - lastSample[0] >= SPACING) {
			this.sample(this.live.clone());
		}
	}

	/**
	 * Notes a jump in the year (a command, or a pause starting or ending): the line so far ends at {@code seasonBefore}
	 * and a new one starts at {@code seasonAfter}, so the two are never blended together.
	 */
	public void recordJump(long gameTime, long seasonBefore, long seasonAfter) {
		this.snapshot = null;
		this.record(gameTime, seasonBefore);
		this.closeLine();
		this.close(new long[]{gameTime, seasonBefore});
		this.close(new long[]{gameTime, seasonAfter});
		this.live = new long[]{gameTime, seasonAfter};
		this.snapshot = null;
	}

	/** Where the year was at a game time. */
	public long seasonAt(long gameTime) {
		if (this.snapshot == null) {
			this.snapshot = this.points();
		}
		List<long[]> all = this.snapshot;
		if (all.isEmpty()) {
			return gameTime;
		}
		long[] first = all.getFirst();
		if (gameTime <= first[0]) {
			return first[1] - (first[0] - gameTime);
		}
		long[] last = all.getLast();
		if (gameTime >= last[0]) {
			return last[1];
		}
		// The last point at or before the time (for a jump, the position after it)
		int low = 0;
		int high = all.size() - 1;
		while (low < high) {
			int middle = (low + high + 1) >>> 1;
			if (all.get(middle)[0] <= gameTime) {
				low = middle;
			} else {
				high = middle - 1;
			}
		}
		return interpolate(all.get(low), all.get(low + 1), gameTime);
	}

	public int size() {
		return this.points().size();
	}

	private void sample(long[] sample) {
		long[] start = this.points.getLast();
		boolean fits = this.line.size() < MAX_LINE_SAMPLES && Math.abs(sample[1] - start[1]) <= Long.MAX_VALUE / 4;
		if (fits) {
			for (long[] earlier : this.line) {
				if (Math.abs(interpolate(start, sample, earlier[0]) - earlier[1]) > TOLERANCE) {
					fits = false;
					break;
				}
			}
		}
		if (!fits) {
			// The line ended at the previous sample; this one starts the next
			this.closeLine();
		}
		this.line.add(sample);
	}

	private void closeLine() {
		if (!this.line.isEmpty()) {
			this.close(this.line.getLast());
			this.line.clear();
		}
	}

	private void close(long[] point) {
		if (!this.points.isEmpty() && same(this.points.getLast(), point)) {
			return;
		}
		this.points.add(point.clone());
		if (this.points.size() > MAX_POINTS) {
			this.points.subList(0, this.points.size() - MAX_POINTS).clear();
		}
	}

	private long[] newest() {
		if (this.live != null) {
			return this.live;
		}
		return this.points.isEmpty() ? null : this.points.getLast();
	}

	private static boolean same(long[] a, long[] b) {
		return a[0] == b[0] && a[1] == b[1];
	}

	private static long interpolate(long[] from, long[] to, long gameTime) {
		long span = to[0] - from[0];
		if (span <= 0) {
			return to[1];
		}
		double share = (double) (gameTime - from[0]) / span;
		return from[1] + Math.round((to[1] - from[1]) * share);
	}
}
