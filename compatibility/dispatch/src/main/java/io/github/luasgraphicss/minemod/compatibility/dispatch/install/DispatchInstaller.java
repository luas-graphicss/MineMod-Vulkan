package io.github.luasgraphicss.minemod.compatibility.dispatch.install;

import io.github.luasgraphicss.minemod.compatibility.dispatch.binding.BindingGeneration;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public final class DispatchInstaller {
    private final CapabilityProbe probe;
    private final LookupInstaller installer;
    private final Set<BindingGeneration> installed = EnumSet.noneOf(BindingGeneration.class);

    public DispatchInstaller(CapabilityProbe probe, LookupInstaller installer) {
        this.probe = Objects.requireNonNull(probe, "probe");
        this.installer = Objects.requireNonNull(installer, "installer");
    }

    public synchronized void install(BindingGeneration generation, FunctionLookup lookup) {
        if (installed.contains(generation)) {
            throw new IllegalStateException("the dispatch is already installed for " + generation);
        }
        if (probe.capabilitiesExist(generation)) {
            throw new IllegalStateException(generation.capabilitiesOwner()
                    + " already resolved its function pointers, so the dispatch would be bypassed");
        }
        installer.install(generation, Objects.requireNonNull(lookup, "lookup"));
        installed.add(generation);
    }

    public synchronized boolean isInstalled(BindingGeneration generation) {
        return installed.contains(generation);
    }
}
