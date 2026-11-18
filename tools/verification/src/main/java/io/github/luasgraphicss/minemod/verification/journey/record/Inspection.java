package io.github.luasgraphicss.minemod.verification.journey.record;

import io.github.luasgraphicss.minemod.verification.journey.evidence.LoadedLibraries;
import io.github.luasgraphicss.minemod.verification.journey.evidence.RendererLines;
import io.github.luasgraphicss.minemod.verification.journey.evidence.RendererReport;
import java.util.List;
import java.util.Optional;

public record Inspection(
        String name,
        String dimension,
        long gameTime,
        List<String> debugLines,
        String rendererReport,
        List<String> libraries,
        List<String> selectedPacks) {

    public Inspection {
        debugLines = List.copyOf(debugLines);
        libraries = libraries == null ? null : List.copyOf(libraries);
        selectedPacks = List.copyOf(selectedPacks);
    }

    public Optional<String> world() {
        return Optional.ofNullable(dimension);
    }

    public Optional<RendererReport> renderer() {
        return Optional.ofNullable(rendererReport).flatMap(RendererReport::parse);
    }

    public Optional<RendererLines> rendererLines() {
        return RendererLines.parse(debugLines);
    }

    public Optional<LoadedLibraries> loadedLibraries() {
        return Optional.ofNullable(libraries).map(LoadedLibraries::new);
    }
}
