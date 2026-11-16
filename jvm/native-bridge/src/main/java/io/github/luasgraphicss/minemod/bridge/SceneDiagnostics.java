package io.github.luasgraphicss.minemod.bridge;

public final class SceneDiagnostics {
    private final NativeSceneSession session;

    SceneDiagnostics(NativeSceneSession session) {
        this.session = session;
    }

    public SceneMetrics metrics() {
        long[] fields = new long[SceneMetrics.FIELD_COUNT];
        NativeResults.requireOk(
                session.withOpenAddress(address -> nativeReadMetrics(address, fields)), "reading the scene metrics");
        return new SceneMetrics(fields);
    }

    public String report() {
        return NativeText.decode(session.withOpenAddress(SceneDiagnostics::nativeReport));
    }

    private static native int nativeReadMetrics(long session, long[] metricFields);

    private static native byte[] nativeReport(long session);
}
