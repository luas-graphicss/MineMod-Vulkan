package io.github.luasgraphicss.minemod.compatibility.translation.legacy;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import io.github.luasgraphicss.minemod.compatibility.resources.names.ContextNames;
import io.github.luasgraphicss.minemod.compatibility.resources.names.NameTable;
import io.github.luasgraphicss.minemod.compatibility.resources.names.ObjectDisposer;
import io.github.luasgraphicss.minemod.compatibility.state.error.ErrorLedger;
import java.util.Objects;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

public final class DisplayListNames {
    private static final int NO_LIST = 0;
    private static final int SINGLE_LIST = 1;
    private static final int GL_INVALID_VALUE = 0x0501;
    private static final int GL_OUT_OF_MEMORY = 0x0505;
    private static final ObjectDisposer KEEP_BACKING = (kind, backing) -> { };

    private final Supplier<ContextNames> currentNames;
    private final ProviderLists provider;
    private final ErrorLedger errors;
    private final LongSupplier currentFrame;
    private final ObjectDisposer disposer;
    private int listBase;

    public DisplayListNames(
            Supplier<ContextNames> currentNames, ProviderLists provider, ErrorLedger errors, LongSupplier currentFrame) {
        this.currentNames = Objects.requireNonNull(currentNames, "currentNames");
        this.provider = Objects.requireNonNull(provider, "provider");
        this.errors = Objects.requireNonNull(errors, "errors");
        this.currentFrame = Objects.requireNonNull(currentFrame, "currentFrame");
        this.disposer = (kind, backing) -> provider.delete(backing, SINGLE_LIST);
    }

    public int generate(int range) {
        if (range < 0) {
            errors.raise(GL_INVALID_VALUE);
            return NO_LIST;
        }
        if (range == 0) {
            return NO_LIST;
        }
        int firstBacking = provider.generate(range);
        if (firstBacking == ProviderLists.NO_LISTS) {
            return NO_LIST;
        }
        int first = claimRun(lists(), firstBacking, range);
        if (first == NO_LIST) {
            provider.delete(firstBacking, range);
            errors.raise(GL_OUT_OF_MEMORY);
        }
        return first;
    }

    public void delete(int first, int range) {
        if (range < 0) {
            errors.raise(GL_INVALID_VALUE);
            return;
        }
        NameTable lists = lists();
        for (int name = first; name < first + range; name++) {
            if (lists.contains(name)) {
                lists.delete(name, disposer);
            }
        }
    }

    public boolean isList(int name) {
        return lists().contains(name);
    }

    public void setBase(int base) {
        listBase = base;
    }

    public int base() {
        return listBase;
    }

    public int[] resolveCalls(int[] offsets) {
        NameTable lists = lists();
        long frame = currentFrame.getAsLong();
        int[] backings = new int[offsets.length];
        for (int index = 0; index < offsets.length; index++) {
            int name = listBase + offsets[index];
            if (lists.contains(name)) {
                lists.markUsed(name, frame);
                backings[index] = lists.backingOf(name);
            }
        }
        return backings;
    }

    private static int claimRun(NameTable lists, int firstBacking, int range) {
        int candidate = 1;
        while (true) {
            int claimed = 0;
            while (claimed < range && lists.claim(candidate + claimed, firstBacking + claimed)) {
                claimed++;
            }
            if (claimed == range) {
                return candidate;
            }
            int blocked = candidate + claimed;
            for (int offset = 0; offset < claimed; offset++) {
                lists.delete(candidate + offset, KEEP_BACKING);
            }
            if (!lists.contains(blocked)) {
                return NO_LIST;
            }
            candidate = blocked + 1;
        }
    }

    private NameTable lists() {
        return currentNames.get().table(ObjectKind.DISPLAY_LIST);
    }
}
