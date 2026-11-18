package io.github.luasgraphicss.minemod.adapters.presentation.fixtures;

import io.github.luasgraphicss.minemod.adapters.presentation.owner.PresentationOwners;

public final class InstalledOwners {
    private InstalledOwners() {}

    public static void release() {
        if (PresentationOwners.isInstalled()) {
            PresentationOwners.release(PresentationOwners.current());
        }
    }
}
