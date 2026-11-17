package io.github.luasgraphicss.minemod.compatibility.bridges.contract;

import java.util.List;

public final class ContractVerdict {
    public enum Status {
        SATISFIED,
        MOD_ABSENT,
        VERSION_NOT_COVERED,
        SYMBOLS_MISSING
    }

    private final Status status;
    private final String explanation;

    private ContractVerdict(Status status, String explanation) {
        this.status = status;
        this.explanation = explanation;
    }

    static ContractVerdict satisfied(String modId, String version) {
        return new ContractVerdict(Status.SATISFIED, modId + " " + version + " keeps every symbol of the contract");
    }

    static ContractVerdict modAbsent(String modId) {
        return new ContractVerdict(Status.MOD_ABSENT, modId + " is not installed");
    }

    static ContractVerdict versionNotCovered(String modId, String version) {
        return new ContractVerdict(
                Status.VERSION_NOT_COVERED, modId + " " + version + " is not a version the contract was verified on");
    }

    static ContractVerdict symbolsMissing(String modId, String version, List<ContractSymbol> missing) {
        return new ContractVerdict(Status.SYMBOLS_MISSING, modId + " " + version + " does not declare " + missing);
    }

    public Status status() {
        return status;
    }

    public String explanation() {
        return explanation;
    }

    public boolean activates() {
        return status == Status.SATISFIED;
    }

    public boolean leavesInstalledModUnbridged() {
        return status == Status.VERSION_NOT_COVERED || status == Status.SYMBOLS_MISSING;
    }

    @Override
    public String toString() {
        return status + ": " + explanation;
    }
}
