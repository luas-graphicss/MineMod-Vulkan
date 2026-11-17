package io.github.luasgraphicss.minemod.verification.adapters.storage;

import io.github.luasgraphicss.minemod.adapters.transformers.callsite.CallSiteRedirect;
import io.github.luasgraphicss.minemod.adapters.transformers.release.ReleasePlan;
import io.github.luasgraphicss.minemod.adapters.transformers.release.ReleasePlanFile;
import io.github.luasgraphicss.minemod.verification.adapters.names.RuntimeNamespace;
import io.github.luasgraphicss.minemod.verification.adapters.plan.AdapterPlan;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.stream.Collectors;

public final class AdapterPlanFile {
    private static final long MAX_BYTES = 256L * 1024;
    private static final String NAMES = "names";
    private static final String NAMES_SOURCE = "names.source";
    private static final String NAMES_SHA1 = "names.sha1";

    private AdapterPlanFile() {}

    public static AdapterPlan read(Path file) throws IOException {
        if (Files.size(file) > MAX_BYTES) {
            throw new IOException(file + " exceeds " + MAX_BYTES + " bytes");
        }
        byte[] content = Files.readAllBytes(file);
        Properties entries = new Properties();
        entries.load(new StringReader(new String(content, StandardCharsets.UTF_8)));
        try {
            ReleasePlan plan = ReleasePlanFile.read(new ByteArrayInputStream(content));
            String namespaceId = entries.getProperty(NAMES, "");
            RuntimeNamespace namespace = RuntimeNamespace.withId(namespaceId)
                    .orElseThrow(() -> new IllegalArgumentException("unknown names: " + namespaceId));
            return new AdapterPlan(
                    plan, namespace, entries.getProperty(NAMES_SOURCE, ""), entries.getProperty(NAMES_SHA1, ""));
        } catch (IllegalArgumentException malformed) {
            throw new IOException(file + ": " + malformed.getMessage(), malformed);
        }
    }

    public static String render(AdapterPlan adapterPlan) {
        ReleasePlan plan = adapterPlan.plan();
        StringBuilder text = new StringBuilder();
        line(text, "release", plan.release());
        line(text, "client.sha1", plan.clientSha1());
        line(text, NAMES, adapterPlan.namespace().id());
        if (adapterPlan.namespace().isReadFromMappings()) {
            line(text, NAMES_SOURCE, adapterPlan.namesUrl());
            line(text, NAMES_SHA1, adapterPlan.namesSha1());
        }
        line(
                text,
                "points",
                plan.redirects().stream().map(redirect -> redirect.point().id()).collect(Collectors.joining(",")));
        for (CallSiteRedirect redirect : plan.redirects()) {
            String key = "point." + redirect.point().id();
            line(text, key + ".callee", redirect.callee().toString());
            line(text, key + ".within", redirect.point().target().toString());
            line(text, key + ".matches", Integer.toString(redirect.point().expectedMatches()));
        }
        return text.toString();
    }

    private static void line(StringBuilder text, String key, String value) {
        text.append(key).append('=').append(value).append('\n');
    }
}
