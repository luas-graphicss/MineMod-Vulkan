package io.github.luasgraphicss.minemod.compatibility.translation.support;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import io.github.luasgraphicss.minemod.compatibility.state.error.ProviderErrors;
import io.github.luasgraphicss.minemod.compatibility.state.submission.AttachmentHandoff;
import io.github.luasgraphicss.minemod.compatibility.translation.legacy.ProviderLists;
import io.github.luasgraphicss.minemod.compatibility.translation.provider.ProviderCalls;
import io.github.luasgraphicss.minemod.compatibility.translation.provider.ProviderObjects;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class RecordingProvider
        implements ProviderCalls, ProviderObjects, ProviderLists, ProviderErrors, AttachmentHandoff {
    public static final int FIRST_BACKING = 100;
    public static final String RELEASE = "handoff: release to provider";
    public static final String RECLAIM = "handoff: reclaim for renderer";
    public static final String FINISH = "handoff: finish provider";

    private final List<String> events = new ArrayList<>();
    private final List<String> disposed = new ArrayList<>();
    private int nextBacking = FIRST_BACKING;

    @Override
    public long invoke(String function, long[] arguments) {
        events.add(function + Arrays.toString(arguments));
        return 0;
    }

    @Override
    public int create(ObjectKind kind, int parameter) {
        return nextBacking++;
    }

    @Override
    public void dispose(ObjectKind kind, int backing) {
        disposed.add(kind + " " + backing);
    }

    @Override
    public int generate(int range) {
        int first = nextBacking;
        nextBacking += range;
        return first;
    }

    @Override
    public void delete(int firstBacking, int range) {
        disposed.add("lists " + firstBacking + "+" + range);
    }

    @Override
    public int takeError() {
        return 0;
    }

    @Override
    public void releaseToProvider() {
        events.add(RELEASE);
    }

    @Override
    public void reclaimForRenderer() {
        events.add(RECLAIM);
    }

    @Override
    public void finishProviderWork() {
        events.add(FINISH);
    }

    public List<String> events() {
        return events;
    }

    public List<String> disposed() {
        return disposed;
    }
}
