package io.github.luasgraphicss.minemod.adapters.transformers.application;

public final class PointApplication {
    private final String pointId;
    private final int matches;

    public PointApplication(String pointId, int matches) {
        if (matches < 0) {
            throw new IllegalArgumentException("point " + pointId + " cannot match " + matches + " times");
        }
        this.pointId = pointId;
        this.matches = matches;
    }

    public String pointId() {
        return pointId;
    }

    public int matches() {
        return matches;
    }
}
