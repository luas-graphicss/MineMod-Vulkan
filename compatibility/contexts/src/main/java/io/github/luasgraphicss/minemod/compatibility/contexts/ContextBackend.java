package io.github.luasgraphicss.minemod.compatibility.contexts;

public interface ContextBackend {
    long NO_SHARED_CONTEXT = 0;

    long create(long sharedWith);

    void makeCurrent(long handle);

    void releaseCurrent();

    void destroy(long handle);
}
