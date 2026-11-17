package io.github.luasgraphicss.minemod.verification.journey.instance;

public enum GameInstance {
    REFERENCE("reference", "runReference"),
    CANDIDATE("candidate", "runCandidate");

    private final String label;
    private final String task;

    GameInstance(String label, String task) {
        this.label = label;
        this.task = task;
    }

    public String label() {
        return label;
    }

    public String task() {
        return task;
    }
}
