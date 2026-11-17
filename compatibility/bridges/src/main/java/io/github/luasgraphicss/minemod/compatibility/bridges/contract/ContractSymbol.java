package io.github.luasgraphicss.minemod.compatibility.bridges.contract;

import java.util.Objects;

public final class ContractSymbol {
    private final String owner;
    private final String name;
    private final String descriptor;

    public ContractSymbol(String owner, String name, String descriptor) {
        if (owner.trim().isEmpty() || name.trim().isEmpty() || !descriptor.startsWith("(")) {
            throw new IllegalArgumentException(
                    "a contract symbol is a method with its owner and descriptor: " + owner + "#" + name + descriptor);
        }
        this.owner = owner;
        this.name = name;
        this.descriptor = descriptor;
    }

    public String owner() {
        return owner;
    }

    public String name() {
        return name;
    }

    public String descriptor() {
        return descriptor;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ContractSymbol)) {
            return false;
        }
        ContractSymbol symbol = (ContractSymbol) other;
        return owner.equals(symbol.owner) && name.equals(symbol.name) && descriptor.equals(symbol.descriptor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(owner, name, descriptor);
    }

    @Override
    public String toString() {
        return owner + "#" + name + descriptor;
    }
}
