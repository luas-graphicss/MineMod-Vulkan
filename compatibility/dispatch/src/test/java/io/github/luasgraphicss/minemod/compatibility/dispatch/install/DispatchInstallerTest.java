package io.github.luasgraphicss.minemod.compatibility.dispatch.install;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.compatibility.dispatch.binding.BindingGeneration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DispatchInstallerTest {
    private final Set<BindingGeneration> withCapabilities = EnumSet.noneOf(BindingGeneration.class);
    private final List<BindingGeneration> installs = new ArrayList<>();
    private final FunctionLookup lookup = function -> FunctionLookup.UNAVAILABLE;
    private final DispatchInstaller installer =
            new DispatchInstaller(withCapabilities::contains, (generation, installed) -> installs.add(generation));

    @Test
    void theLookupIsInstalledBeforeTheBindingResolvesItsPointers() {
        installer.install(BindingGeneration.LWJGL3, lookup);

        assertEquals(Collections.singletonList(BindingGeneration.LWJGL3), installs);
        assertTrue(installer.isInstalled(BindingGeneration.LWJGL3));
    }

    @Test
    void aBindingThatAlreadyHasCapabilitiesIsRefused() {
        withCapabilities.add(BindingGeneration.LWJGL2);

        IllegalStateException refusal = assertThrows(
                IllegalStateException.class, () -> installer.install(BindingGeneration.LWJGL2, lookup));

        assertTrue(refusal.getMessage().startsWith("org.lwjgl.opengl.GLContext"));
        assertEquals(Collections.emptyList(), installs);
        assertFalse(installer.isInstalled(BindingGeneration.LWJGL2));
    }

    @Test
    void eachGenerationIsInstalledOnceAndOnItsOwn() {
        installer.install(BindingGeneration.LWJGL2, lookup);

        assertThrows(IllegalStateException.class, () -> installer.install(BindingGeneration.LWJGL2, lookup));
        installer.install(BindingGeneration.LWJGL3, lookup);

        assertEquals(2, installs.size());
    }
}
