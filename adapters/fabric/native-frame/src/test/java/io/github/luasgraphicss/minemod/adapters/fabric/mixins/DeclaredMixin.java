package io.github.luasgraphicss.minemod.adapters.fabric.mixins;

import java.util.List;

record DeclaredMixin(String name, List<String> targets) {
    DeclaredMixin {
        targets = List.copyOf(targets);
    }
}
