package io.github.luasgraphicss.minemod.bridge;

import io.github.luasgraphicss.minemod.contract.abi.AbiResult;

public final class SceneReload {
    private final NativeSceneSession session;

    SceneReload(NativeSceneSession session) {
        this.session = session;
    }

    public void begin() {
        NativeResults.requireOk(session.withOpenAddress(SceneReload::nativeBegin), "beginning a resource reload");
    }

    public boolean stageProgram(int program, int[] vertexWords, int[] fragmentWords) {
        if (program <= 0) {
            throw new IllegalArgumentException("a staged program takes a positive id, found " + program);
        }
        int resultCode =
                session.withOpenAddress(address -> nativeStageProgram(address, program, vertexWords, fragmentWords));
        return isAccepted(resultCode, "staging program " + program);
    }

    public boolean commit() {
        return isAccepted(session.withOpenAddress(SceneReload::nativeCommit), "publishing the staged resources");
    }

    public void abort() {
        NativeResults.requireOk(session.withOpenAddress(SceneReload::nativeAbort), "abandoning the staged resources");
    }

    public String report() {
        return NativeText.decode(session.withOpenAddress(SceneReload::nativeReport));
    }

    private static boolean isAccepted(int resultCode, String operation) {
        if (AbiResult.fromCode(resultCode) == AbiResult.INVALID_ARGUMENT) {
            return false;
        }
        NativeResults.requireOk(resultCode, operation);
        return true;
    }

    private static native int nativeBegin(long session);

    private static native int nativeStageProgram(long session, int program, int[] vertexWords, int[] fragmentWords);

    private static native int nativeCommit(long session);

    private static native int nativeAbort(long session);

    private static native byte[] nativeReport(long session);
}
