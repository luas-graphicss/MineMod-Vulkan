package io.github.luasgraphicss.minemod.adapters.frame.lists;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DisplayLists<C> {
    private static final int NO_LIST = 0;

    private final Map<Integer, List<C>> compiled = new HashMap<>();
    private List<C> compiling;
    private int compilingName;
    private int lastName;

    public int generate(int range) {
        if (range < 1) {
            return NO_LIST;
        }
        int first = lastName + 1;
        lastName += range;
        return first;
    }

    public void begin(int list) {
        compilingName = list;
        compiling = new ArrayList<>();
    }

    public boolean isCompiling() {
        return compiling != null;
    }

    public void record(C command) {
        if (compiling == null) {
            throw new IllegalStateException("no display list is being compiled");
        }
        compiling.add(command);
    }

    public void end() {
        if (compiling != null) {
            compiled.put(compilingName, Collections.unmodifiableList(compiling));
            compiling = null;
        }
    }

    public List<C> commandsOf(int list) {
        List<C> commands = compiled.get(list);
        return commands == null ? Collections.<C>emptyList() : commands;
    }

    public void delete(int first, int range) {
        for (int list = first; list < first + range; list++) {
            compiled.remove(list);
        }
    }
}
