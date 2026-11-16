package io.github.luasgraphicss.minemod.client.shaders;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class ShaderImports {
    private static final String DIRECTIVE = "moj_import";
    private static final String INCLUDE_DIRECTORY = "shaders/include/";
    private static final String CORE_DIRECTORY = "shaders/core/";
    private static final int MAX_KEY_LENGTH = 256;
    private static final int SHORTEST_KEY = 3;

    private ShaderImports() {}

    public static List<String> keysIn(String source) {
        Set<String> keys = new LinkedHashSet<>();
        int lineStart = 0;
        while (lineStart < source.length()) {
            int newline = source.indexOf('\n', lineStart);
            int lineEnd = newline < 0 ? source.length() : newline;
            keyIn(source, lineStart, lineEnd).ifPresent(keys::add);
            lineStart = lineEnd + 1;
        }
        return new ArrayList<>(keys);
    }

    public static Optional<String> pathOf(String key) {
        if (key.length() < SHORTEST_KEY) {
            return Optional.empty();
        }
        String name = key.substring(1, key.length() - 1);
        if (!isPlainRelativePath(name)) {
            return Optional.empty();
        }
        return Optional.of((key.charAt(0) == '<' ? INCLUDE_DIRECTORY : CORE_DIRECTORY) + name);
    }

    private static Optional<String> keyIn(String source, int lineStart, int lineEnd) {
        int cursor = afterBlanks(source, lineStart, lineEnd);
        if (cursor == lineEnd || source.charAt(cursor) != '#') {
            return Optional.empty();
        }
        cursor = afterBlanks(source, cursor + 1, lineEnd);
        if (!source.startsWith(DIRECTIVE, cursor)) {
            return Optional.empty();
        }
        cursor = afterBlanks(source, cursor + DIRECTIVE.length(), lineEnd);
        if (cursor >= lineEnd) {
            return Optional.empty();
        }
        char opening = source.charAt(cursor);
        if (opening != '<' && opening != '"') {
            return Optional.empty();
        }
        int closing = source.indexOf(opening == '<' ? '>' : '"', cursor + 1);
        boolean closesOnTheLine = closing > cursor + 1 && closing < lineEnd && closing - cursor < MAX_KEY_LENGTH;
        return closesOnTheLine ? Optional.of(source.substring(cursor, closing + 1)) : Optional.empty();
    }

    private static int afterBlanks(String source, int from, int lineEnd) {
        int cursor = from;
        while (cursor < lineEnd && (source.charAt(cursor) == ' ' || source.charAt(cursor) == '\t')) {
            cursor++;
        }
        return cursor;
    }

    private static boolean isPlainRelativePath(String name) {
        for (String segment : name.split("/", -1)) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
                return false;
            }
            for (int index = 0; index < segment.length(); index++) {
                if (!isPathCharacter(segment.charAt(index))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isPathCharacter(char symbol) {
        return (symbol >= 'a' && symbol <= 'z')
                || (symbol >= '0' && symbol <= '9')
                || symbol == '_'
                || symbol == '-'
                || symbol == '.';
    }
}
