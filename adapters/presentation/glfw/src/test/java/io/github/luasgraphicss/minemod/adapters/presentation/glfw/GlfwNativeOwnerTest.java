package io.github.luasgraphicss.minemod.adapters.presentation.glfw;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.luasgraphicss.minemod.adapters.presentation.fixtures.InstalledOwners;
import io.github.luasgraphicss.minemod.adapters.presentation.owner.PresentationOwner;
import io.github.luasgraphicss.minemod.adapters.presentation.owner.PresentationOwners;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class GlfwNativeOwnerTest {
    @AfterEach
    void releaseTheOwner() {
        InstalledOwners.release();
    }

    @Test
    void presentsTheMainWindowInsteadOfTheLibraryOfTheGame() {
        PresentationOwner owner = GlfwNativeOwner.installFrom(GlfwNativeOwnerTest.class.getClassLoader());

        assertSame(owner, PresentationOwners.current());
        assertFalse(owner.keepsOpenGlContext());
    }

    @Test
    void leavesTheMainWindowToTheOwnerAlreadyInstalled() {
        PresentationOwner first = GlfwNativeOwner.installFrom(GlfwNativeOwnerTest.class.getClassLoader());

        assertThrows(
                IllegalStateException.class,
                () -> GlfwNativeOwner.installFrom(GlfwNativeOwnerTest.class.getClassLoader()));
        assertSame(first, PresentationOwners.current());
    }
}
