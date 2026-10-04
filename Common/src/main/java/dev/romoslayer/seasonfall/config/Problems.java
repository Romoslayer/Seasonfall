package dev.romoslayer.seasonfall.config;

import java.util.ArrayList;
import java.util.List;

/** Values in the config file that were not usable as written, and what was used instead. */
public final class Problems {
	private final List<String> messages = new ArrayList<>();

	public void add(String message) {
		this.messages.add(message);
	}

	public List<String> messages() {
		return List.copyOf(this.messages);
	}

	public boolean isEmpty() {
		return this.messages.isEmpty();
	}
}
