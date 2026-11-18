package io.github.luasgraphicss.minemod.journey.inspection;

import java.util.List;

public record Inspection(
        String name,
        String dimension,
        long gameTime,
        List<String> debugLines,
        String rendererReport,
        List<String> libraries,
        List<String> selectedPacks) {}
