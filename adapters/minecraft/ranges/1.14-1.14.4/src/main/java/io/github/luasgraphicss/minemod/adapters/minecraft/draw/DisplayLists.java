package io.github.luasgraphicss.minemod.adapters.minecraft.draw;

import java.util.HashMap;
import java.util.Map;

public final class DisplayLists {
    private static final DisplayLists CURRENT = new DisplayLists();
    private static final int FIRST_NAME = 1;

    private final Map<Integer, DisplayList> compiled = new HashMap<>();
    private int nextName = FIRST_NAME;
    private int compilingName;
    private DisplayList compiling;

    private DisplayLists() {}

    public static DisplayLists current() {
        return CURRENT;
    }

    public int generate(int count) {
        int first = nextName;
        nextName += count;
        return first;
    }

    public void delete(int first, int count) {
        for (int name = first; name < first + count; name++) {
            compiled.remove(name);
        }
    }

    public void begin(int name) {
        compilingName = name;
        compiling = new DisplayList();
    }

    public void end() {
        if (compiling != null) {
            compiled.put(compilingName, compiling);
            compiling = null;
        }
    }

    public void call(int name) {
        DisplayList list = compiled.get(name);
        if (list != null) {
            list.record();
        }
    }

    boolean isCompiling() {
        return compiling != null;
    }

    void keep(DrawCapture.Geometry geometry) {
        compiling.keep(geometry);
    }
}
