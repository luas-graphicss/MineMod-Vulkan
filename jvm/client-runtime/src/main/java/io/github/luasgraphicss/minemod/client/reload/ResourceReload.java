package io.github.luasgraphicss.minemod.client.reload;

import io.github.luasgraphicss.minemod.bridge.CoreShaderSource;
import io.github.luasgraphicss.minemod.bridge.ShaderCompilation;
import io.github.luasgraphicss.minemod.client.shaders.ProgramCatalog;
import io.github.luasgraphicss.minemod.client.shaders.ShaderCompiler;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public final class ResourceReload implements AutoCloseable {
    public static final int MAX_PROGRAMS = 1024;

    private final ProgramStaging staging;
    private final ShaderCompiler compiler;
    private final ProgramCatalog programs = new ProgramCatalog();
    private final AtomicLong requests = new AtomicLong();
    private volatile ReloadOutcome lastOutcome = ReloadOutcome.notReloaded();

    public ResourceReload(ProgramStaging staging, ShaderCompiler compiler) {
        this.staging = staging;
        this.compiler = compiler;
    }

    public PreparedReload prepare(List<CoreShaderSource> overrides) {
        long request = requests.incrementAndGet();
        List<PreparedProgram> compiled = new ArrayList<>();
        List<String> problems = new ArrayList<>();
        if (overrides.size() > MAX_PROGRAMS) {
            problems.add("resource packs replace " + overrides.size() + " core shaders, more than the "
                    + MAX_PROGRAMS + " a generation holds");
            return new PreparedReload(this, request, compiled, problems);
        }
        for (CoreShaderSource override : overrides) {
            ShaderCompilation compilation = compiler.compile(override);
            if (compilation.succeeded()) {
                compiled.add(new PreparedProgram(
                        override.name(), compilation.vertexWords(), compilation.fragmentWords()));
            } else {
                problems.add(override.name() + ": " + compilation.log().trim());
            }
        }
        return new PreparedReload(this, request, compiled, problems);
    }

    public ReloadOutcome publish(PreparedReload prepared) {
        if (!prepared.isRequest(this, requests.get())) {
            return ReloadOutcome.superseded();
        }
        ReloadOutcome outcome = prepared.problems().isEmpty()
                ? stageAndCommit(prepared)
                : ReloadOutcome.rejected(String.join("\n", prepared.problems()));
        lastOutcome = outcome;
        return outcome;
    }

    public ProgramCatalog programs() {
        return programs;
    }

    public ReloadOutcome lastOutcome() {
        return lastOutcome;
    }

    @Override
    public void close() {
        compiler.close();
    }

    private ReloadOutcome stageAndCommit(PreparedReload prepared) {
        Set<String> staged = new LinkedHashSet<>();
        staging.begin();
        try {
            for (PreparedProgram program : prepared.programs()) {
                int id = programs.idOf(program.shader());
                if (!staging.stage(id, program.vertexWords(), program.fragmentWords())) {
                    String refusal = staging.report().trim();
                    staging.abort();
                    return ReloadOutcome.rejected(program.shader() + ": " + refusal);
                }
                staged.add(program.shader());
            }
        } catch (RuntimeException failure) {
            staging.abort();
            throw failure;
        }
        if (!staging.commit()) {
            return ReloadOutcome.rejected(staging.report().trim());
        }
        programs.publish(staged);
        return ReloadOutcome.published(staged.size());
    }
}
