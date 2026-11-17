package io.github.luasgraphicss.minemod.adapters.shaderpacks.pipeline;

import io.github.luasgraphicss.minemod.adapters.shaderpacks.targets.BufferSide;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.targets.PackTarget;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

final class BufferFlips {
    private final Set<PackTarget> flipped = new TreeSet<>();

    BufferSide readSide(PackTarget target) {
        return flipped.contains(target) ? BufferSide.ALT : BufferSide.MAIN;
    }

    List<PackTarget> leftFlipped() {
        return new ArrayList<>(flipped);
    }

    void flip(PackTarget target) {
        if (!flipped.remove(target)) {
            flipped.add(target);
        }
    }
}
