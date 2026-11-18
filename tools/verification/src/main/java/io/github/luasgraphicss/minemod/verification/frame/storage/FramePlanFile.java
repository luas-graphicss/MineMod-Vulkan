package io.github.luasgraphicss.minemod.verification.frame.storage;

import io.github.luasgraphicss.minemod.adapters.loaders.launch.plan.FramePlan;
import io.github.luasgraphicss.minemod.adapters.transformers.callsite.CallSiteRedirect;
import io.github.luasgraphicss.minemod.verification.adapters.plan.AdapterPlan;
import io.github.luasgraphicss.minemod.verification.adapters.storage.AdapterPlanFile;
import io.github.luasgraphicss.minemod.verification.frame.catalog.StandIns;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FramePlanFile {
    public record Committed(AdapterPlan names, FramePlan atLaunch) {}

    private static final String RESOURCE = "src/main/resources/" + FramePlan.RESOURCE;

    private FramePlanFile() {}

    public static Path of(Path repositoryRoot, String module) {
        return repositoryRoot.resolve(module).resolve(RESOURCE);
    }

    public static Committed read(Path file) throws IOException {
        AdapterPlan names = AdapterPlanFile.read(file);
        try (InputStream content = Files.newInputStream(file)) {
            return new Committed(names, FramePlan.read(content));
        } catch (IllegalArgumentException malformed) {
            throw new IOException(file + ": " + malformed.getMessage(), malformed);
        }
    }

    public static String render(AdapterPlan plan, StandIns standIns) {
        StringBuilder text = new StringBuilder(AdapterPlanFile.render(plan));
        for (CallSiteRedirect redirect : plan.plan().redirects()) {
            String standIn = standIns.answering(redirect.callee().toString())
                    .orElseThrow(() -> new IllegalArgumentException("no stand-in answers " + redirect.callee()));
            text.append("point.")
                    .append(redirect.point().id())
                    .append(".hook=")
                    .append(standIn)
                    .append('\n');
        }
        return text.toString();
    }
}
