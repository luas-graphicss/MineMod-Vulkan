package io.github.luasgraphicss.minemod.adapters.presentation.owner;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class PresentationOwners {
    private static final PresentationOwner LIBRARY = new LibraryPresentation();
    private static final AtomicReference<PresentationOwner> INSTALLED = new AtomicReference<>();

    private PresentationOwners() {}

    public static void install(PresentationOwner owner) {
        Objects.requireNonNull(owner, "owner");
        if (!INSTALLED.compareAndSet(null, owner)) {
            throw new IllegalStateException(
                    "the main window is presented by " + INSTALLED.get() + "; " + owner + " cannot present it too");
        }
    }

    public static void release(PresentationOwner owner) {
        if (!INSTALLED.compareAndSet(owner, null)) {
            throw new IllegalStateException(owner + " does not present the main window");
        }
    }

    public static PresentationOwner current() {
        PresentationOwner installed = INSTALLED.get();
        return installed == null ? LIBRARY : installed;
    }

    public static boolean isInstalled() {
        return INSTALLED.get() != null;
    }
}
