package io.github.luasgraphicss.minemod.client.shaders;

import io.github.luasgraphicss.minemod.bridge.CoreShaderSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class CoreShaderOverrides {
    private static final String CORE_DIRECTORY = "shaders/core/";
    private static final String VERTEX_EXTENSION = ".vsh";
    private static final String FRAGMENT_EXTENSION = ".fsh";

    private CoreShaderOverrides() {}

    public static List<CoreShaderSource> collect(ShaderFiles files, List<ProgramStages> programs) {
        List<CoreShaderSource> overrides = new ArrayList<>();
        for (ProgramStages program : programs) {
            overrideOf(files, program).ifPresent(overrides::add);
        }
        return overrides;
    }

    private static Optional<CoreShaderSource> overrideOf(ShaderFiles files, ProgramStages program) {
        Optional<ShaderFile> vertex = files.open(CORE_DIRECTORY + program.vertexStage() + VERTEX_EXTENSION);
        Optional<ShaderFile> fragment = files.open(CORE_DIRECTORY + program.fragmentStage() + FRAGMENT_EXTENSION);
        if (!vertex.isPresent() || !fragment.isPresent()) {
            return Optional.empty();
        }
        IncludeCollector includes = new IncludeCollector(files);
        includes.collectFrom(vertex.get().text());
        includes.collectFrom(fragment.get().text());
        boolean replacesBuiltIn = program.isRedefined()
                || vertex.get().replacesBuiltIn()
                || fragment.get().replacesBuiltIn()
                || includes.replacesBuiltIn();
        if (!replacesBuiltIn) {
            return Optional.empty();
        }
        return Optional.of(
                new CoreShaderSource(program.name(), vertex.get().text(), fragment.get().text(), includes.texts()));
    }
}
