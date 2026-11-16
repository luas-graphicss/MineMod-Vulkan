package io.github.luasgraphicss.minemod.compatibility.interop.negotiation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SharingOutcome {
    private final List<PlannedShare> shares;
    private final List<String> rejections;

    private SharingOutcome(List<PlannedShare> shares, List<String> rejections) {
        this.shares = Collections.unmodifiableList(new ArrayList<>(shares));
        this.rejections = Collections.unmodifiableList(new ArrayList<>(rejections));
    }

    static SharingOutcome accepted(List<PlannedShare> shares) {
        return new SharingOutcome(shares, Collections.<String>emptyList());
    }

    static SharingOutcome rejected(List<String> rejections) {
        return new SharingOutcome(Collections.<PlannedShare>emptyList(), rejections);
    }

    public boolean isAccepted() {
        return rejections.isEmpty();
    }

    public List<PlannedShare> shares() {
        return shares;
    }

    public List<String> rejections() {
        return rejections;
    }
}
