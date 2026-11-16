package io.github.luasgraphicss.minemod.compatibility.resources.names;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;

public final class NameTable {
    private static final long NEVER_USED = -1;
    private static final int[] NO_BACKINGS = new int[0];

    private final ObjectKind kind;
    private final boolean[] live;
    private final int[] backings;
    private final long[] lastUses;
    private final Map<Integer, Integer> namesByBacking = new HashMap<>();
    private final List<RetiringBacking> retiring = new ArrayList<>();
    private int freeCandidate;
    private int liveCount;

    public NameTable(ObjectKind kind, int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("a name table needs room for at least one name: " + capacity);
        }
        this.kind = Objects.requireNonNull(kind, "kind");
        this.live = new boolean[capacity];
        this.backings = new int[capacity];
        this.lastUses = new long[capacity];
    }

    public ObjectKind kind() {
        return kind;
    }

    public synchronized int allocate(int backing) {
        requireUnnamed(backing);
        int index = freeCandidate;
        while (index < live.length && live[index]) {
            index++;
        }
        if (index == live.length) {
            throw new IllegalStateException("no free " + kind + " name among " + live.length);
        }
        occupy(index, backing);
        freeCandidate = index + 1;
        return index + 1;
    }

    public synchronized boolean claim(int name, int backing) {
        if (name < 1 || name > live.length || live[name - 1]) {
            return false;
        }
        requireUnnamed(backing);
        occupy(name - 1, backing);
        return true;
    }

    public synchronized boolean contains(int name) {
        return name >= 1 && name <= live.length && live[name - 1];
    }

    public synchronized int backingOf(int name) {
        return backings[liveIndex(name)];
    }

    public synchronized OptionalInt nameOf(int backing) {
        Integer name = namesByBacking.get(backing);
        return name == null ? OptionalInt.empty() : OptionalInt.of(name);
    }

    public synchronized void markUsed(int name, long frame) {
        if (frame < 0) {
            throw new IllegalArgumentException("frame index out of range: " + frame);
        }
        int index = liveIndex(name);
        if (frame > lastUses[index]) {
            lastUses[index] = frame;
        }
    }

    public void rebind(int name, int backing, ObjectDisposer disposer) {
        int replaced;
        synchronized (this) {
            int index = liveIndex(name);
            requireUnnamed(backing);
            replaced = backings[index];
            long replacedLastUse = lastUses[index];
            namesByBacking.remove(replaced);
            namesByBacking.put(backing, name);
            backings[index] = backing;
            lastUses[index] = NEVER_USED;
            if (replacedLastUse != NEVER_USED) {
                retiring.add(new RetiringBacking(replaced, replacedLastUse));
                return;
            }
        }
        disposer.dispose(kind, replaced);
    }

    public void delete(int name, ObjectDisposer disposer) {
        int backing;
        synchronized (this) {
            int index = liveIndex(name);
            backing = backings[index];
            long lastUse = lastUses[index];
            namesByBacking.remove(backing);
            live[index] = false;
            liveCount--;
            if (index < freeCandidate) {
                freeCandidate = index;
            }
            if (lastUse != NEVER_USED) {
                retiring.add(new RetiringBacking(backing, lastUse));
                return;
            }
        }
        disposer.dispose(kind, backing);
    }

    public int collect(long completedFrame, ObjectDisposer disposer) {
        int[] released;
        synchronized (this) {
            released = takeRetiredUpTo(completedFrame);
        }
        for (int backing : released) {
            disposer.dispose(kind, backing);
        }
        return released.length;
    }

    public synchronized int discardAll() {
        int discarded = liveCount + retiring.size();
        Arrays.fill(live, false);
        namesByBacking.clear();
        retiring.clear();
        liveCount = 0;
        freeCandidate = 0;
        return discarded;
    }

    public synchronized int liveCount() {
        return liveCount;
    }

    public synchronized int retiringCount() {
        return retiring.size();
    }

    private int[] takeRetiredUpTo(long completedFrame) {
        if (retiring.isEmpty()) {
            return NO_BACKINGS;
        }
        int[] released = new int[retiring.size()];
        int releasedCount = 0;
        int keptCount = 0;
        for (RetiringBacking candidate : retiring) {
            if (candidate.lastUse <= completedFrame) {
                released[releasedCount++] = candidate.backing;
            } else {
                retiring.set(keptCount++, candidate);
            }
        }
        retiring.subList(keptCount, retiring.size()).clear();
        return Arrays.copyOf(released, releasedCount);
    }

    private int liveIndex(int name) {
        if (name < 1 || name > live.length || !live[name - 1]) {
            throw new IllegalArgumentException(kind + " name is not in use: " + name);
        }
        return name - 1;
    }

    private void requireUnnamed(int backing) {
        if (namesByBacking.containsKey(backing)) {
            throw new IllegalArgumentException(kind + " backing already has a name: " + backing);
        }
    }

    private void occupy(int index, int backing) {
        live[index] = true;
        backings[index] = backing;
        lastUses[index] = NEVER_USED;
        namesByBacking.put(backing, index + 1);
        liveCount++;
    }

    private static final class RetiringBacking {
        private final int backing;
        private final long lastUse;

        private RetiringBacking(int backing, long lastUse) {
            this.backing = backing;
            this.lastUse = lastUse;
        }
    }
}
