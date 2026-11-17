package io.github.luasgraphicss.minemod.verification.hooks.scan;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.luasgraphicss.minemod.verification.hooks.catalog.LibraryCall;
import io.github.luasgraphicss.minemod.verification.hooks.catalog.PresentationCalls;
import io.github.luasgraphicss.minemod.verification.hooks.fixtures.FixtureClient;
import io.github.luasgraphicss.minemod.verification.hooks.model.ReleaseHooks.Windowing;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CallSiteScannerTest {
    private static final List<LibraryCall> GLFW_CALLS = PresentationCalls.of(Windowing.GLFW);
    private static final LibraryCall CREATE = GLFW_CALLS.get(0);
    private static final LibraryCall SWAP = GLFW_CALLS.get(3);
    private static final LibraryCall UNLISTED =
            new LibraryCall("poll", "org/lwjgl/glfw/GLFW", "glfwPollEvents", "()V");

    @Test
    void findsEveryListedCallAndTheMethodAroundIt() {
        byte[] window = FixtureClient.classCalling(
                "a", Map.of("b", List.of(SWAP, UNLISTED, SWAP), "c", List.of(CREATE)));

        List<CallSite> sites = new CallSiteScanner(GLFW_CALLS).scan(window);

        assertEquals(
                List.of("a.b()V swap-buffers", "a.b()V swap-buffers", "a.c()V window-create"),
                sites.stream().map(site -> site.within() + " " + site.call().hook()).sorted().toList());
    }

    @Test
    void scansOnlyTheClassesOfAnArchiveThatNameTheLibrary(@TempDir Path directory) throws IOException {
        Path jar = FixtureClient.jarOf(
                directory.resolve("client.jar"),
                Map.of(
                        "a", FixtureClient.classCalling("a", Map.of("b", List.of(SWAP))),
                        "d", FixtureClient.classCalling("d", Map.of("e", List.of()))));

        try (ClientArchive archive = ClientArchive.open(jar)) {
            assertEquals(List.of("a", "d"), archive.classNames());
            assertEquals(
                    List.of(new CallSite(SWAP, "a", "b", "()V")), new CallSiteScanner(GLFW_CALLS).scan(archive));
        }
    }
}
