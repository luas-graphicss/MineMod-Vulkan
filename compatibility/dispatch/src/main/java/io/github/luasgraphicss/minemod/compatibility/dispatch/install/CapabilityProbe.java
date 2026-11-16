package io.github.luasgraphicss.minemod.compatibility.dispatch.install;

import io.github.luasgraphicss.minemod.compatibility.dispatch.binding.BindingGeneration;

public interface CapabilityProbe {
    boolean capabilitiesExist(BindingGeneration generation);
}
