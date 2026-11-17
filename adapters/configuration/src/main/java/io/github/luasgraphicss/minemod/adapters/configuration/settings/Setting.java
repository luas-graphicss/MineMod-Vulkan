package io.github.luasgraphicss.minemod.adapters.configuration.settings;

import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

public final class Setting<T> {
    private static final Pattern INTEGER = Pattern.compile("-?[0-9]{1,9}");
    private static final Pattern FILE_NAME = Pattern.compile("[^/\\\\\\p{Cntrl}]{0,255}");

    private final String key;
    private final T fallback;
    private final Function<String, Optional<T>> parser;
    private final String accepted;

    private Setting(String key, T fallback, Function<String, Optional<T>> parser, String accepted) {
        this.key = key;
        this.fallback = fallback;
        this.parser = parser;
        this.accepted = accepted;
    }

    public static Setting<Boolean> flag(String key, boolean fallback) {
        return new Setting<>(key, fallback, Setting::parseFlag, "true or false");
    }

    public static Setting<Integer> ranged(String key, int fallback, int minimum, int maximum) {
        return new Setting<>(
                key, fallback, text -> parseRanged(text, minimum, maximum), minimum + " to " + maximum);
    }

    public static Setting<String> fileName(String key) {
        return new Setting<>(key, "", Setting::parseFileName, "a file name without directories");
    }

    public String key() {
        return key;
    }

    public T fallback() {
        return fallback;
    }

    public String accepted() {
        return accepted;
    }

    public Optional<T> parse(String text) {
        return parser.apply(text.trim());
    }

    private static Optional<Boolean> parseFlag(String text) {
        if (text.equals("true")) {
            return Optional.of(Boolean.TRUE);
        }
        return text.equals("false") ? Optional.of(Boolean.FALSE) : Optional.<Boolean>empty();
    }

    private static Optional<Integer> parseRanged(String text, int minimum, int maximum) {
        if (!INTEGER.matcher(text).matches()) {
            return Optional.empty();
        }
        int value = Integer.parseInt(text);
        return value >= minimum && value <= maximum ? Optional.of(value) : Optional.<Integer>empty();
    }

    private static Optional<String> parseFileName(String text) {
        boolean namesADirectory = text.equals(".") || text.equals("..");
        return FILE_NAME.matcher(text).matches() && !namesADirectory ? Optional.of(text) : Optional.<String>empty();
    }
}
