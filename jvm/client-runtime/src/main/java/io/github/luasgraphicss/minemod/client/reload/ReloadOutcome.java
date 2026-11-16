package io.github.luasgraphicss.minemod.client.reload;

import java.util.Objects;

public final class ReloadOutcome {
    public enum Status {
        NOT_RELOADED,
        PUBLISHED,
        REJECTED,
        SUPERSEDED
    }

    private static final ReloadOutcome NOT_RELOADED = new ReloadOutcome(Status.NOT_RELOADED, 0, "");
    private static final ReloadOutcome SUPERSEDED = new ReloadOutcome(Status.SUPERSEDED, 0, "");

    private final Status status;
    private final int replacedPrograms;
    private final String report;

    private ReloadOutcome(Status status, int replacedPrograms, String report) {
        this.status = status;
        this.replacedPrograms = replacedPrograms;
        this.report = Objects.requireNonNull(report, "report");
    }

    public static ReloadOutcome notReloaded() {
        return NOT_RELOADED;
    }

    public static ReloadOutcome published(int replacedPrograms) {
        return new ReloadOutcome(Status.PUBLISHED, replacedPrograms, "");
    }

    public static ReloadOutcome rejected(String report) {
        return new ReloadOutcome(Status.REJECTED, 0, report);
    }

    public static ReloadOutcome superseded() {
        return SUPERSEDED;
    }

    public Status status() {
        return status;
    }

    public int replacedPrograms() {
        return replacedPrograms;
    }

    public String report() {
        return report;
    }

    public boolean keptThePreviousGeneration() {
        return status == Status.REJECTED || status == Status.SUPERSEDED;
    }
}
