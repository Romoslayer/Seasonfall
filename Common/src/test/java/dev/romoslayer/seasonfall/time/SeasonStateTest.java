package dev.romoslayer.seasonfall.time;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SeasonStateTest {
	@TempDir
	Path folder;

	private final SeasonState.Mover normalMover = SeasonState.mover;

	@AfterEach
	void restoreMover() {
		SeasonState.mover = this.normalMover;
	}

	private static SeasonState state(long ticks, boolean paused) {
		SeasonState state = new SeasonState();
		state.seasonTicks = ticks;
		state.paused = paused;
		state.history = List.of(new long[]{10, 20}, new long[]{30, 40});
		return state;
	}

	@Test
	void savesAndLoads() {
		Path file = this.folder.resolve("seasonfall.json");
		assertTrue(state(1234, true).save(file));
		SeasonState loaded = SeasonState.load(file);
		assertNotNull(loaded);
		assertEquals(1234, loaded.seasonTicks);
		assertTrue(loaded.paused);
		assertEquals(2, loaded.history.size());
		assertEquals(40, loaded.history.get(1)[1]);
	}

	@Test
	void failedSaveReportsFailureAndKeepsTheOldFile() throws IOException {
		Path file = this.folder.resolve("seasonfall.json");
		assertTrue(state(1, false).save(file));
		SeasonState.mover = (from, to, atomic) -> {
			throw new IOException("disk unavailable");
		};
		assertFalse(state(2, true).save(file));
		assertEquals(1, SeasonState.load(file).seasonTicks);
		assertFalse(Files.exists(this.folder.resolve("seasonfall.json.tmp")), "temporary file cleaned up");
		// Storage is back: the same state saves fine on the next try
		SeasonState.mover = this.normalMover;
		assertTrue(state(2, true).save(file));
		assertEquals(2, SeasonState.load(file).seasonTicks);
	}

	@Test
	void filesystemWithoutAtomicMovesStillSaves() {
		Path file = this.folder.resolve("seasonfall.json");
		SeasonState.mover = (from, to, atomic) -> {
			if (atomic) {
				throw new AtomicMoveNotSupportedException(from.toString(), to.toString(), "not here");
			}
			Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
		};
		assertTrue(state(77, false).save(file));
		assertEquals(77, SeasonState.load(file).seasonTicks);
	}

	@Test
	void unreadableFileIsSetAsideNotLost() throws IOException {
		Path file = this.folder.resolve("seasonfall.json");
		Files.writeString(file, "{ this is not json");
		assertNull(SeasonState.load(file));
		assertFalse(Files.exists(file));
		try (Stream<Path> files = Files.list(this.folder)) {
			List<Path> aside = files.filter(path -> path.getFileName().toString().startsWith("seasonfall.json.unreadable-")).toList();
			assertEquals(1, aside.size());
			assertEquals("{ this is not json", Files.readString(aside.get(0)));
		}
	}

	@Test
	void missingFileIsANewWorld() {
		assertNull(SeasonState.load(this.folder.resolve("nothing.json")));
	}
}
