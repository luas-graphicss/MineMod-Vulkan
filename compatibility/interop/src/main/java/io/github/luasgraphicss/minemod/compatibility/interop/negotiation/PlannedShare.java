package io.github.luasgraphicss.minemod.compatibility.interop.negotiation;

public final class PlannedShare {
    private final SharingRequest request;
    private final boolean dedicatedAllocation;

    PlannedShare(SharingRequest request, boolean dedicatedAllocation) {
        this.request = request;
        this.dedicatedAllocation = dedicatedAllocation;
    }

    public SharingRequest request() {
        return request;
    }

    public boolean usesDedicatedAllocation() {
        return dedicatedAllocation;
    }
}
