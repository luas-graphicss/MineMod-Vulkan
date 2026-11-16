package io.github.luasgraphicss.minemod.compatibility.bridges.support;

import io.github.luasgraphicss.minemod.compatibility.spi.BridgeContext;
import io.github.luasgraphicss.minemod.compatibility.spi.BridgeDescriptor;
import io.github.luasgraphicss.minemod.compatibility.spi.RenderBridge;
import io.github.luasgraphicss.minemod.compatibility.spi.SpiVersion;
import java.util.Arrays;
import java.util.List;

public class RecordingBridge implements RenderBridge {
    private final BridgeDescriptor descriptor;
    private final List<String> journal;
    private BridgeContext context;
    private boolean failingDetach;

    public RecordingBridge(String id, int spiVersion, List<String> journal, String... operations) {
        this.descriptor = new BridgeDescriptor(id, spiVersion, Arrays.asList(operations));
        this.journal = journal;
    }

    public RecordingBridge(String id, List<String> journal, String... operations) {
        this(id, SpiVersion.CURRENT, journal, operations);
    }

    @Override
    public BridgeDescriptor descriptor() {
        return descriptor;
    }

    @Override
    public void attach(BridgeContext attachedContext) {
        context = attachedContext;
        journal.add(descriptor.id() + " attached");
    }

    @Override
    public long serve(String operation, long[] arguments) {
        journal.add(descriptor.id() + " serves " + operation + Arrays.toString(arguments));
        return arguments.length;
    }

    @Override
    public void detach() {
        journal.add(descriptor.id() + " detached");
        if (failingDetach) {
            throw new IllegalStateException("resources were still in use");
        }
    }

    public BridgeContext context() {
        return context;
    }

    public void failDetach() {
        failingDetach = true;
    }
}
