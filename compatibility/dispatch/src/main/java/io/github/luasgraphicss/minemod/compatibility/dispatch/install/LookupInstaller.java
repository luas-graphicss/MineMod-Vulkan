package io.github.luasgraphicss.minemod.compatibility.dispatch.install;

import io.github.luasgraphicss.minemod.compatibility.dispatch.binding.BindingGeneration;

public interface LookupInstaller {
    void install(BindingGeneration generation, FunctionLookup lookup);
}
