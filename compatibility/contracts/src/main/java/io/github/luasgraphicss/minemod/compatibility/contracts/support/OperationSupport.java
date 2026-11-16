package io.github.luasgraphicss.minemod.compatibility.contracts.support;

public enum OperationSupport {
    SUPPORTED("supported"),
    BRIDGE_REQUIRED("waiting for a bridge"),
    OUT_OF_CONTRACT("outside the contract"),
    UNVERIFIED("not verified");

    private final String description;

    OperationSupport(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }
}
