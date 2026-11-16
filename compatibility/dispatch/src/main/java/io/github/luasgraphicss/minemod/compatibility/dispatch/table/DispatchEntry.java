package io.github.luasgraphicss.minemod.compatibility.dispatch.table;

import io.github.luasgraphicss.minemod.compatibility.contracts.support.OperationSupport;

public final class DispatchEntry {
    private final String function;
    private final Disposition disposition;
    private final long address;
    private final OperationSupport support;

    DispatchEntry(String function, Disposition disposition, long address, OperationSupport support) {
        this.function = function;
        this.disposition = disposition;
        this.address = address;
        this.support = support;
    }

    public String function() {
        return function;
    }

    public Disposition disposition() {
        return disposition;
    }

    public long address() {
        return address;
    }

    public OperationSupport support() {
        return support;
    }

    @Override
    public String toString() {
        return function + ": " + disposition + ", " + support.description();
    }
}
