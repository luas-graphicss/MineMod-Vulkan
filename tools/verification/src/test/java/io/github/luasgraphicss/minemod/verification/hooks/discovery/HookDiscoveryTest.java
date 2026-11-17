package io.github.luasgraphicss.minemod.verification.hooks.discovery;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.luasgraphicss.minemod.verification.hooks.catalog.LibraryCall;
import io.github.luasgraphicss.minemod.verification.hooks.model.HookPoint;
import io.github.luasgraphicss.minemod.verification.hooks.scan.CallSite;
import java.util.List;
import org.junit.jupiter.api.Test;

class HookDiscoveryTest {
    private static final LibraryCall SWAP =
            new LibraryCall("swap-buffers", "org/lwjgl/glfw/GLFW", "glfwSwapBuffers", "(J)V");
    private static final LibraryCall DESTROY =
            new LibraryCall("window-destroy", "org/lwjgl/glfw/GLFW", "glfwDestroyWindow", "(J)V");
    private static final String SWAP_SIGNATURE = "org/lwjgl/glfw/GLFW.glfwSwapBuffers(J)V";

    @Test
    void namesAPointAfterItsHookWhenOneMethodCallsIt() {
        List<CallSite> sites = List.of(new CallSite(SWAP, "a", "b", "()V"), new CallSite(SWAP, "a", "b", "()V"));

        assertEquals(
                List.of(new HookPoint("swap-buffers", "swap-buffers", SWAP_SIGNATURE, "a.b()V", "named", 2)),
                HookDiscovery.pointsOf(List.of(SWAP), sites, site -> "named"));
    }

    @Test
    void numbersThePointsOfAHookCalledFromSeveralMethods() {
        List<CallSite> sites = List.of(
                new CallSite(SWAP, "a", "c", "()V"),
                new CallSite(SWAP, "a", "b", "()V"),
                new CallSite(SWAP, "a", "c", "()V"));

        assertEquals(
                List.of(
                        new HookPoint("swap-buffers#1", "swap-buffers", SWAP_SIGNATURE, "a.b()V", null, 1),
                        new HookPoint("swap-buffers#2", "swap-buffers", SWAP_SIGNATURE, "a.c()V", null, 2)),
                HookDiscovery.pointsOf(List.of(SWAP), sites, site -> null));
    }

    @Test
    void pinsNothingForACallTheClientNeverMakes() {
        List<CallSite> sites = List.of(new CallSite(SWAP, "a", "b", "()V"));

        assertEquals(List.of(), HookDiscovery.pointsOf(List.of(DESTROY), sites, site -> null));
    }
}
