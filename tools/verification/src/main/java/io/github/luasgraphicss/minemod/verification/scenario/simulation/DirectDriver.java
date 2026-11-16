package io.github.luasgraphicss.minemod.verification.scenario.simulation;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import io.github.luasgraphicss.minemod.compatibility.provider.capability.ContextProfile;
import io.github.luasgraphicss.minemod.compatibility.state.submission.AttachmentOwner;
import java.util.Arrays;
import java.util.List;

public final class DirectDriver implements GlDriver {
    private static final String CALL_LISTS = "glCallLists";

    private final Timeline timeline = new Timeline();
    private final SimulatedProvider provider;
    private int listBase;

    public DirectDriver(ContextProfile profile) {
        provider = new SimulatedProvider(profile, timeline, () -> AttachmentOwner.PROVIDER);
    }

    public SimulatedProvider provider() {
        return provider;
    }

    @Override
    public long call(String function, long... arguments) {
        return provider.invoke(function, arguments);
    }

    @Override
    public int[] generate(ObjectKind kind, int count) {
        int[] names = new int[count];
        for (int index = 0; index < count; index++) {
            names[index] = provider.create(kind, 0);
        }
        return names;
    }

    @Override
    public void delete(ObjectKind kind, int... names) {
        for (int name : names) {
            provider.dispose(kind, name);
        }
    }

    @Override
    public boolean isObject(ObjectKind kind, int name) {
        return provider.isLive(kind, name);
    }

    @Override
    public int generateLists(int range) {
        return provider.generate(range);
    }

    @Override
    public void deleteLists(int first, int range) {
        provider.delete(first, range);
    }

    @Override
    public void setListBase(int base) {
        listBase = base;
    }

    @Override
    public void callLists(int... offsets) {
        provider.invoke(CALL_LISTS, Arrays.stream(offsets).mapToLong(offset -> listBase + offset).toArray());
    }

    @Override
    public int takeError() {
        return provider.takeError();
    }

    @Override
    public List<String> providerCalls() {
        return timeline.from(Timeline.Source.PROVIDER);
    }
}
