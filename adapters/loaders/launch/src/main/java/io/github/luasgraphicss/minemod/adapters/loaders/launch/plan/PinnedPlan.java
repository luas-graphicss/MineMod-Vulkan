package io.github.luasgraphicss.minemod.adapters.loaders.launch.plan;

import io.github.luasgraphicss.minemod.adapters.transformers.release.ReleasePlan;
import io.github.luasgraphicss.minemod.adapters.transformers.release.ReleasePlanFile;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

public final class PinnedPlan {
    public static final String RESOURCE = "META-INF/minemod/release-plan.properties";

    private PinnedPlan() {}

    public static ReleasePlan of(ClassLoader adapter) {
        try (InputStream content = adapter.getResourceAsStream(RESOURCE)) {
            if (content == null) {
                throw new IllegalStateException("the adapter carries no " + RESOURCE);
            }
            return ReleasePlanFile.read(content);
        } catch (IOException unreadable) {
            throw new UncheckedIOException("the release plan of the adapter cannot be read", unreadable);
        }
    }
}
