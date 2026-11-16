package io.github.luasgraphicss.minemod.contract.abi;

public final class AbiException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final AbiResult result;

    public AbiException(AbiResult result, String operation) {
        super(operation + " failed with " + result);
        this.result = result;
    }

    public AbiResult result() {
        return result;
    }
}
