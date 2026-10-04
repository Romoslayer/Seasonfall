package dev.romoslayer.seasonfall.time;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The world's place in the year, saved as seasonfall.json in the world folder. */
public final class SeasonState {
	private static final Logger LOGGER = LoggerFactory.getLogger("Seasonfall");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	/** Ticks of season time since the world's first year began. Moves with the overworld's clock (or commands). */
	public long seasonTicks;
	/** Set by /seasonfall pause: the year stays where it is however much time passes. */
	public boolean paused;
	/** How the year moved against game time recently: [game time, season time] points (see {@link SeasonHistory}). */
	public @Nullable List<long[]> history;

	/** Moves the finished temporary file into place. Replaceable so tests can make it fail. */
	@FunctionalInterface
	interface Mover {
		void move(Path from, Path to, boolean atomic) throws IOException;
	}

	static Mover mover = (from, to, atomic) -> {
		if (atomic) {
			Files.move(from, to, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} else {
			Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
		}
	};

	/**
	 * Reads the saved state, or null when there is none or it cannot be read. A file that exists but cannot be read is
	 * set aside next to it (not overwritten), so nothing is lost if it can be repaired by hand.
	 */
	public static @Nullable SeasonState load(Path file) {
		if (!Files.isRegularFile(file)) {
			return null;
		}
		try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			SeasonState state = GSON.fromJson(reader, SeasonState.class);
			if (state == null) {
				throw new JsonParseException("the file is empty");
			}
			state.seasonTicks = Math.max(0, state.seasonTicks);
			if (state.history == null) {
				state.history = new ArrayList<>();
			}
			return state;
		} catch (IOException | JsonParseException | IllegalStateException e) {
			Path aside = file.resolveSibling(file.getFileName() + ".unreadable-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")));
			try {
				Files.move(file, aside);
				LOGGER.error("Could not read the season from {} ({}); kept it as {} and started a new year", file, e.getMessage(), aside.getFileName());
			} catch (IOException moveError) {
				LOGGER.error("Could not read the season from {} ({}), nor set it aside; starting a new year", file, e.getMessage());
			}
			return null;
		}
	}

	/**
	 * Writes the state to a temporary file, then swaps it in, so the previous file stays whole until the new one is
	 * complete. Returns whether it worked; nothing is thrown.
	 */
	public boolean save(Path file) {
		Path temp = file.resolveSibling(file.getFileName() + ".tmp");
		try {
			try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
				GSON.toJson(this, writer);
			}
			try {
				mover.move(temp, file, true);
			} catch (AtomicMoveNotSupportedException e) {
				// Some filesystems cannot swap atomically; a plain replace still never leaves a half-written file
				mover.move(temp, file, false);
			}
			return true;
		} catch (IOException | RuntimeException e) {
			LOGGER.debug("Saving the season to {} failed", file, e);
			try {
				Files.deleteIfExists(temp);
			} catch (IOException ignored) {
				// Left for the next attempt to overwrite
			}
			return false;
		}
	}
}
