package io.github.luasgraphicss.minemod.compatibility.provider.capability;

import io.github.luasgraphicss.minemod.compatibility.provider.identity.ProviderIdentity;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public final class ProviderCapabilities {
    private final ProviderIdentity identity;
    private final Set<String> extensions;

    public ProviderCapabilities(ProviderIdentity identity, Collection<String> extensions) {
        this.identity = Objects.requireNonNull(identity, "identity");
        this.extensions = Collections.unmodifiableSet(new TreeSet<>(extensions));
    }

    public ProviderIdentity identity() {
        return identity;
    }

    public Set<String> extensions() {
        return extensions;
    }
}
