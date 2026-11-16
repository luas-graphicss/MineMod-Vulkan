package io.github.luasgraphicss.minemod.compatibility.dispatch.catalog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class BraceExpansion {
    private static final char OPEN = '{';
    private static final char CLOSE = '}';
    private static final String SEPARATOR = ",";

    private BraceExpansion() {
    }

    static List<String> expand(String pattern) {
        List<String> expanded = Collections.singletonList("");
        int position = 0;
        while (position < pattern.length()) {
            int open = pattern.indexOf(OPEN, position);
            if (open < 0) {
                return append(expanded, new String[] {pattern.substring(position)});
            }
            int close = pattern.indexOf(CLOSE, open);
            if (close < 0) {
                throw new IllegalArgumentException("alternatives are never closed in " + pattern);
            }
            expanded = append(expanded, new String[] {pattern.substring(position, open)});
            expanded = append(expanded, pattern.substring(open + 1, close).split(SEPARATOR, -1));
            position = close + 1;
        }
        return expanded;
    }

    private static List<String> append(List<String> prefixes, String[] alternatives) {
        List<String> combined = new ArrayList<>(prefixes.size() * alternatives.length);
        for (String prefix : prefixes) {
            for (String alternative : alternatives) {
                combined.add(prefix + alternative);
            }
        }
        return combined;
    }
}
