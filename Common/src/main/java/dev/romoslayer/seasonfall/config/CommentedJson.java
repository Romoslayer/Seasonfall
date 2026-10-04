package dev.romoslayer.seasonfall.config;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Writes JSON with a // comment above every option that has a {@link Comment}. Gson's parser reads such comments back
 * without complaint, so the file stays plain JSON otherwise.
 */
final class CommentedJson {
	private static final String INDENT = "  ";

	private final Gson gson;
	private final StringBuilder out = new StringBuilder();

	private CommentedJson(Gson gson) {
		this.gson = gson;
	}

	static String write(Gson gson, Object config) {
		CommentedJson writer = new CommentedJson(gson);
		writer.value(gson.toJsonTree(config), config, 0);
		writer.out.append('\n');
		return writer.out.toString();
	}

	private void value(JsonElement element, @Nullable Object source, int depth) {
		if (element.isJsonObject()) {
			this.object(element.getAsJsonObject(), source, depth);
		} else if (element.isJsonArray()) {
			this.array(element.getAsJsonArray(), depth);
		} else {
			this.out.append(this.gson.toJson(element));
		}
	}

	private void object(JsonObject object, @Nullable Object source, int depth) {
		if (object.isEmpty()) {
			this.out.append("{}");
			return;
		}
		// Small objects of plain values (a colour and a strength, say) read better on one line
		if (isFlat(object) && source != null && !(source instanceof Map<?, ?>) && commentFor(source, object.keySet().iterator().next()) == null) {
			this.out.append('{');
			boolean first = true;
			for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
				this.out.append(first ? " " : ", ").append(this.gson.toJson(entry.getKey())).append(": ").append(this.gson.toJson(entry.getValue()));
				first = false;
			}
			this.out.append(" }");
			return;
		}
		this.out.append("{\n");
		Iterator<Map.Entry<String, JsonElement>> entries = object.entrySet().iterator();
		while (entries.hasNext()) {
			Map.Entry<String, JsonElement> entry = entries.next();
			String comment = source == null || source instanceof Map<?, ?> ? null : commentFor(source, entry.getKey());
			if (comment != null) {
				for (String line : comment.split("\n")) {
					this.indent(depth + 1).append("// ").append(line).append('\n');
				}
			}
			this.indent(depth + 1).append(this.gson.toJson(entry.getKey())).append(": ");
			this.value(entry.getValue(), child(source, entry.getKey()), depth + 1);
			this.out.append(entries.hasNext() ? ",\n" : "\n");
		}
		this.indent(depth).append('}');
	}

	private void array(JsonArray array, int depth) {
		this.out.append(this.gson.toJson(array));
	}

	private StringBuilder indent(int depth) {
		return this.out.append(INDENT.repeat(depth));
	}

	private static boolean isFlat(JsonObject object) {
		return object.size() <= 4 && object.entrySet().stream().allMatch(e -> e.getValue().isJsonPrimitive());
	}

	private static @Nullable Object child(@Nullable Object source, String key) {
		if (source instanceof Map<?, ?> map) {
			return map.get(key);
		}
		if (source == null || source instanceof Collection<?>) {
			return null;
		}
		Field field = field(source, key);
		if (field == null) {
			return null;
		}
		try {
			return field.get(source);
		} catch (IllegalAccessException e) {
			return null;
		}
	}

	private static @Nullable String commentFor(Object source, String key) {
		Field field = field(source, key);
		Comment comment = field == null ? null : field.getAnnotation(Comment.class);
		return comment == null ? null : comment.value();
	}

	private static @Nullable Field field(Object source, String key) {
		for (Class<?> type = source.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
			try {
				Field field = type.getDeclaredField(key);
				if (!Modifier.isStatic(field.getModifiers())) {
					field.setAccessible(true);
					return field;
				}
			} catch (NoSuchFieldException ignored) {
				// keep looking in the superclass
			}
		}
		return null;
	}
}
