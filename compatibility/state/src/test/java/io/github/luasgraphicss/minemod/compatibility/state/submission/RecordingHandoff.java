package io.github.luasgraphicss.minemod.compatibility.state.submission;

import java.util.ArrayList;
import java.util.List;

final class RecordingHandoff implements AttachmentHandoff {
    static final String RELEASE = "release";
    static final String RECLAIM = "reclaim";
    static final String FINISH = "finish";

    private final List<String> calls = new ArrayList<>();
    private boolean refusingRelease;

    @Override
    public void releaseToProvider() {
        if (refusingRelease) {
            throw new IllegalStateException("the attachments could not be exported");
        }
        calls.add(RELEASE);
    }

    @Override
    public void reclaimForRenderer() {
        calls.add(RECLAIM);
    }

    @Override
    public void finishProviderWork() {
        calls.add(FINISH);
    }

    void refuseRelease() {
        refusingRelease = true;
    }

    List<String> calls() {
        return calls;
    }
}
