package io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.reload;

import com.mojang.logging.LogUtils;
import io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.bootstrap.ClientRendering;
import io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.shaders.PackPrograms;
import io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.shaders.PackShaderFiles;
import io.github.luasgraphicss.minemod.client.reload.PreparedReload;
import io.github.luasgraphicss.minemod.client.reload.ReloadOutcome;
import io.github.luasgraphicss.minemod.client.shaders.CoreShaderOverrides;
import java.util.Optional;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

public final class CoreShaderReloadListener extends SimplePreparableReloadListener<Optional<PreparedReload>> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String PUBLISHED_MESSAGE = "Resource packs replace {} core shader(s) of the native renderer";
    private static final String REJECTED_MESSAGE =
            "The native renderer keeps its previous shaders, the reload was refused:\n{}";

    @Override
    protected Optional<PreparedReload> prepare(ResourceManager resources, ProfilerFiller profiler) {
        return ClientRendering.running()
                .map(renderer -> renderer.reload()
                        .prepare(CoreShaderOverrides.collect(
                                new PackShaderFiles(resources), PackPrograms.definedIn(resources))));
    }

    @Override
    protected void apply(Optional<PreparedReload> prepared, ResourceManager resources, ProfilerFiller profiler) {
        prepared.ifPresent(reload -> ClientRendering.running()
                .ifPresent(renderer -> report(renderer.reload().publish(reload))));
    }

    private static void report(ReloadOutcome outcome) {
        switch (outcome.status()) {
            case PUBLISHED -> LOGGER.info(PUBLISHED_MESSAGE, outcome.replacedPrograms());
            case REJECTED -> LOGGER.warn(REJECTED_MESSAGE, outcome.report());
            default -> {}
        }
    }
}
