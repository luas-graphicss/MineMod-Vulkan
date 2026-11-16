package io.github.luasgraphicss.minemod.compatibility.layer.capability;

import io.github.luasgraphicss.minemod.compatibility.layer.memory.NativeMemory;
import io.github.luasgraphicss.minemod.compatibility.layer.memory.NativeText;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

public final class ExposedExtensions implements AutoCloseable {
    private final NativeMemory memory;
    private final List<String> names;
    private final long[] nameAddresses;
    private final long joinedAddress;
    private boolean closed;

    public ExposedExtensions(NativeMemory memory, Collection<String> exposed) {
        this.memory = Objects.requireNonNull(memory, "memory");
        this.names = Collections.unmodifiableList(new ArrayList<>(new TreeSet<>(exposed)));
        this.nameAddresses = new long[names.size()];
        for (int index = 0; index < nameAddresses.length; index++) {
            nameAddresses[index] = NativeText.allocate(memory, names.get(index));
        }
        this.joinedAddress = NativeText.allocate(memory, String.join(" ", names));
    }

    public List<String> names() {
        return names;
    }

    public int count() {
        return names.size();
    }

    public long nameAt(int index) {
        return nameAddresses[index];
    }

    public long joined() {
        return joinedAddress;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        for (long address : nameAddresses) {
            memory.free(address);
        }
        memory.free(joinedAddress);
    }
}
