package io.github.luasgraphicss.minemod.verification.adapters.names;

import java.util.Arrays;
import java.util.Optional;

public enum RuntimeNamespace {
    OFFICIAL("official", false),
    MOJANG("mojang", false),
    INTERMEDIARY("intermediary", true),
    SRG("srg", true);

    private final String id;
    private final boolean readFromMappings;

    RuntimeNamespace(String id, boolean readFromMappings) {
        this.id = id;
        this.readFromMappings = readFromMappings;
    }

    public String id() {
        return id;
    }

    public boolean isReadFromMappings() {
        return readFromMappings;
    }

    public static Optional<RuntimeNamespace> withId(String id) {
        return Arrays.stream(values()).filter(namespace -> namespace.id.equals(id)).findFirst();
    }
}
