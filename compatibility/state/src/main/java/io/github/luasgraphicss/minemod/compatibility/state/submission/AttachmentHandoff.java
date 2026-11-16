package io.github.luasgraphicss.minemod.compatibility.state.submission;

public interface AttachmentHandoff {
    void releaseToProvider();

    void reclaimForRenderer();

    void finishProviderWork();
}
