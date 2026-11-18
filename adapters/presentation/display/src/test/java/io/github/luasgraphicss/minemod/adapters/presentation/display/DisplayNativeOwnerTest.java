package io.github.luasgraphicss.minemod.adapters.presentation.display;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import io.github.luasgraphicss.minemod.adapters.presentation.fixtures.InstalledOwners;
import io.github.luasgraphicss.minemod.adapters.presentation.owner.PresentationOwner;
import io.github.luasgraphicss.minemod.adapters.presentation.owner.PresentationOwners;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class DisplayNativeOwnerTest {
    @AfterEach
    void releaseTheOwner() {
        InstalledOwners.release();
    }

    @Test
    void presentsTheDisplayInsteadOfTheLibraryOfTheGame() {
        PresentationOwner owner = DisplayNativeOwner.installFrom(DisplayNativeOwnerTest.class.getClassLoader());

        assertSame(owner, PresentationOwners.current());
        assertFalse(owner.keepsOpenGlContext());
        assertEquals("the native renderer on a stand-in for the LWJGL 2 display", owner.toString());
    }
}
