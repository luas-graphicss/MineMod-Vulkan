package io.github.luasgraphicss.minemod.verification.ecosystem.shaders;

import io.github.luasgraphicss.minemod.adapters.shaderpacks.pack.ShaderPack;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.pipeline.FramePlanner;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.pipeline.PipelineCapabilities;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.pipeline.PlanningOutcome;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.reader.PackReader;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.uniforms.StandardUniforms;
import java.io.IOException;
import java.nio.file.Path;

public final class FixturePacks {
    public static final String CHAINED_PASSES = "chained-passes";
    public static final PipelineCapabilities WITHOUT_COMPUTE = new PipelineCapabilities(false, 8, 8);
    public static final PipelineCapabilities WITH_COMPUTE = new PipelineCapabilities(true, 16, 8);

    private static final String REPOSITORY_DIRECTORY = "tests/fixtures/shader-packs";
    private static final String SHADERS_DIRECTORY = "shaders";

    private final Path directory;

    public FixturePacks(Path repositoryRoot) {
        this.directory = repositoryRoot.resolve(REPOSITORY_DIRECTORY);
    }

    public PlanningOutcome plan(String pack, PipelineCapabilities capabilities) throws IOException {
        ShaderPack read = PackReader.read(pack, directory.resolve(pack).resolve(SHADERS_DIRECTORY));
        return new FramePlanner(StandardUniforms.bundled(), capabilities).plan(read);
    }
}
