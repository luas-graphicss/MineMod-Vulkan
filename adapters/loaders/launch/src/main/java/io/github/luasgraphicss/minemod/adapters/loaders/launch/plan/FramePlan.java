package io.github.luasgraphicss.minemod.adapters.loaders.launch.plan;

import io.github.luasgraphicss.minemod.adapters.loaders.launch.apply.ClassFileApplication;
import io.github.luasgraphicss.minemod.adapters.loaders.launch.apply.ClassFileApplications;
import io.github.luasgraphicss.minemod.adapters.loaders.launch.apply.ClassNodeApplication;
import io.github.luasgraphicss.minemod.adapters.loaders.launch.apply.ClassNodeApplications;
import io.github.luasgraphicss.minemod.adapters.transformers.callsite.CallSiteRedirect;
import io.github.luasgraphicss.minemod.adapters.transformers.release.ReleasePlan;
import io.github.luasgraphicss.minemod.adapters.transformers.release.ReleasePlanFile;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

public final class FramePlan {
    public static final String RESOURCE = "META-INF/minemod/frame-plan.properties";
    private static final String POINT_PREFIX = "point.";
    private static final String STAND_IN = ".hook";
    private static final Pattern INTERNAL_NAME = Pattern.compile("[^;\\[./]+(?:/[^;\\[./]+)*");
    private static final int BUFFER_BYTES = 8 * 1024;

    private final String release;
    private final String clientSha1;
    private final Map<String, ReleasePlan> plansByStandIn;

    private FramePlan(String release, String clientSha1, Map<String, ReleasePlan> plansByStandIn) {
        this.release = release;
        this.clientSha1 = clientSha1;
        this.plansByStandIn = Collections.unmodifiableMap(plansByStandIn);
    }

    public static FramePlan of(ClassLoader adapter) {
        try (InputStream content = adapter.getResourceAsStream(RESOURCE)) {
            if (content == null) {
                throw new IllegalStateException("the adapter carries no " + RESOURCE);
            }
            return read(content);
        } catch (IOException unreadable) {
            throw new UncheckedIOException("the frame plan of the adapter cannot be read", unreadable);
        }
    }

    public static FramePlan read(InputStream content) throws IOException {
        byte[] bytes = bytesOf(content);
        ReleasePlan whole = ReleasePlanFile.read(new ByteArrayInputStream(bytes));
        Properties entries = new Properties();
        entries.load(new StringReader(new String(bytes, StandardCharsets.UTF_8)));
        Map<String, List<CallSiteRedirect>> redirectsByStandIn = new LinkedHashMap<>();
        for (CallSiteRedirect redirect : whole.redirects()) {
            String standIn = standInOf(entries, redirect.point().id());
            List<CallSiteRedirect> answered = redirectsByStandIn.get(standIn);
            if (answered == null) {
                answered = new ArrayList<>();
                redirectsByStandIn.put(standIn, answered);
            }
            answered.add(redirect);
        }
        Map<String, ReleasePlan> plansByStandIn = new LinkedHashMap<>();
        for (Map.Entry<String, List<CallSiteRedirect>> answered : redirectsByStandIn.entrySet()) {
            plansByStandIn.put(
                    answered.getKey(), new ReleasePlan(whole.release(), whole.clientSha1(), answered.getValue()));
        }
        return new FramePlan(whole.release(), whole.clientSha1(), plansByStandIn);
    }

    public Map<String, ReleasePlan> plansByStandIn() {
        return plansByStandIn;
    }

    public ClassFileApplications classFilesAfter(ReleasePlan presentation, String presentationHooks) {
        requirePinnedLike(presentation);
        List<ClassFileApplication> applications = new ArrayList<>();
        applications.add(new ClassFileApplication(presentation, presentationHooks));
        for (Map.Entry<String, ReleasePlan> answered : plansByStandIn.entrySet()) {
            applications.add(new ClassFileApplication(answered.getValue(), answered.getKey()));
        }
        return new ClassFileApplications(applications);
    }

    public ClassNodeApplications classNodesAfter(ReleasePlan presentation, String presentationHooks) {
        requirePinnedLike(presentation);
        List<ClassNodeApplication> applications = new ArrayList<>();
        applications.add(new ClassNodeApplication(presentation, presentationHooks));
        for (Map.Entry<String, ReleasePlan> answered : plansByStandIn.entrySet()) {
            applications.add(new ClassNodeApplication(answered.getValue(), answered.getKey()));
        }
        return new ClassNodeApplications(applications);
    }

    private void requirePinnedLike(ReleasePlan presentation) {
        if (!presentation.release().equals(release) || !presentation.isPinnedFor(clientSha1)) {
            throw new IllegalStateException("the frame plan was pinned on client " + clientSha1 + " of " + release
                    + ", the release plan on client " + presentation.clientSha1() + " of " + presentation.release());
        }
    }

    private static String standInOf(Properties entries, String point) {
        String key = POINT_PREFIX + point + STAND_IN;
        String standIn = entries.getProperty(key, "").trim();
        if (!INTERNAL_NAME.matcher(standIn).matches()) {
            throw new IllegalArgumentException(key + " names no class that answers the call: " + standIn);
        }
        return standIn;
    }

    private static byte[] bytesOf(InputStream content) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[BUFFER_BYTES];
        for (int read = content.read(buffer); read != -1; read = content.read(buffer)) {
            bytes.write(buffer, 0, read);
        }
        return bytes.toByteArray();
    }
}
