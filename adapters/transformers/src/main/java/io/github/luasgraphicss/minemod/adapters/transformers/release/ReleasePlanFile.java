package io.github.luasgraphicss.minemod.adapters.transformers.release;

import io.github.luasgraphicss.minemod.adapters.transformers.callsite.CallSiteRedirect;
import io.github.luasgraphicss.minemod.adapters.transformers.point.MethodTarget;
import io.github.luasgraphicss.minemod.adapters.transformers.point.TransformPoint;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public final class ReleasePlanFile {
    private static final int MAX_BYTES = 256 * 1024;
    private static final int BUFFER_BYTES = 8 * 1024;
    private static final String RELEASE = "release";
    private static final String CLIENT_SHA1 = "client.sha1";
    private static final String POINTS = "points";
    private static final String POINT_PREFIX = "point.";
    private static final String CALLEE = ".callee";
    private static final String WITHIN = ".within";
    private static final String MATCHES = ".matches";

    private final Properties entries;

    private ReleasePlanFile(Properties entries) {
        this.entries = entries;
    }

    public static ReleasePlan read(InputStream content) throws IOException {
        Properties entries = new Properties();
        entries.load(new StringReader(new String(boundedBytesOf(content), StandardCharsets.UTF_8)));
        return new ReleasePlanFile(entries).plan();
    }

    private ReleasePlan plan() {
        List<CallSiteRedirect> redirects = new ArrayList<>();
        for (String id : required(POINTS).split(",")) {
            redirects.add(redirectOf(id.trim()));
        }
        return new ReleasePlan(required(RELEASE), required(CLIENT_SHA1), redirects);
    }

    private CallSiteRedirect redirectOf(String id) {
        String key = POINT_PREFIX + id;
        TransformPoint point = TransformPoint.hook(id, methodOf(key + WITHIN), matchesOf(key + MATCHES));
        return new CallSiteRedirect(point, methodOf(key + CALLEE));
    }

    private MethodTarget methodOf(String key) {
        try {
            return MethodTarget.parse(required(key));
        } catch (IllegalArgumentException malformed) {
            throw new IllegalArgumentException(key + ": " + malformed.getMessage(), malformed);
        }
    }

    private int matchesOf(String key) {
        String matches = required(key);
        try {
            return Integer.parseInt(matches);
        } catch (NumberFormatException malformed) {
            throw new IllegalArgumentException(key + " is not a number of matches: " + matches, malformed);
        }
    }

    private String required(String key) {
        String value = entries.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("the plan has no " + key);
        }
        return value.trim();
    }

    private static byte[] boundedBytesOf(InputStream content) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[BUFFER_BYTES];
        for (int read = content.read(buffer); read != -1; read = content.read(buffer)) {
            bytes.write(buffer, 0, read);
            if (bytes.size() > MAX_BYTES) {
                throw new IOException("a release plan exceeds " + MAX_BYTES + " bytes");
            }
        }
        return bytes.toByteArray();
    }
}
