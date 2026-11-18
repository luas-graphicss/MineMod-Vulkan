package io.github.luasgraphicss.minemod.adapters.loaders.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.adapters.loaders.launch.fixtures.FixtureGame;
import io.github.luasgraphicss.minemod.adapters.loaders.launch.plan.FramePlan;
import io.github.luasgraphicss.minemod.adapters.loaders.launch.plan.PlanRefusedException;
import io.github.luasgraphicss.minemod.adapters.presentation.fixtures.InstalledOwners;
import io.github.luasgraphicss.minemod.adapters.presentation.glfw.GlfwPresentationHooks;
import io.github.luasgraphicss.minemod.adapters.presentation.owner.PresentationOwners;
import io.github.luasgraphicss.minemod.adapters.transformers.release.ReleasePlan;
import io.github.luasgraphicss.minemod.adapters.transformers.release.ReleasePlanFile;
import io.github.luasgraphicss.minemod.adapters.transformers.release.UnsupportedClientException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ClientBootstrapAgentTest {
    private static final String MAIN_CLASS_ENTRY = "net/minecraft/client/main/Main.class";
    private static final String WINDOW_ENTRY = FixtureGame.WINDOW + ".class";

    private final List<ClassFileTransformer> transformers = new ArrayList<>();
    private final List<PlanRefusedException> refusals = new ArrayList<>();

    @TempDir
    Path directory;

    @AfterEach
    void releaseTheOwner() {
        InstalledOwners.release();
    }

    @Test
    void installsTheNativeOwnerOnceTheClientIsAccepted() throws IOException {
        Path client = clientHolding("client.jar", pinnedWindow());
        assertFalse(PresentationOwners.isInstalled());

        install(planPinnedTo(client), framePlanPinnedTo(client), classPathOf(client));

        assertTrue(PresentationOwners.isInstalled());
        assertFalse(PresentationOwners.current().keepsOpenGlContext());
    }

    @Test
    void redirectsThePinnedCallsOfTheClientOnTheClassPath() throws IOException, IllegalClassFormatException {
        byte[] window = pinnedWindow();
        Path client = clientHolding("client.jar", window);

        install(planPinnedTo(client), framePlanPinnedTo(client), classPathOf(client));

        byte[] rewritten = transformers.get(0).transform(null, FixtureGame.WINDOW, null, null, window);
        assertEquals(1, FixtureGame.callsTo(rewritten, GlfwPresentationHooks.INTERNAL_NAME));
        assertEquals(0, FixtureGame.callsTo(rewritten, FixtureGame.SWAP_BUFFERS.owner()));
        assertEquals(1, FixtureGame.callsTo(rewritten, FixtureGame.STAND_IN));
        assertEquals(0, FixtureGame.callsTo(rewritten, FixtureGame.MATRIX_MODE.owner()));
        assertNull(transformers.get(0).transform(null, "fixture/Screen", null, null, window));
        assertEquals(new ArrayList<>(), refusals);
    }

    @Test
    void refusesAClientThatIsNotThePinnedOne() throws IOException {
        Path pinned = clientHolding("pinned.jar", pinnedWindow());
        Path other = clientHolding("other.jar", windowSwappingTwice());

        assertThrows(
                UnsupportedClientException.class,
                () -> install(planPinnedTo(pinned), framePlanPinnedTo(pinned), classPathOf(other)));
        assertTrue(transformers.isEmpty());
        assertFalse(PresentationOwners.isInstalled());
    }

    @Test
    void refusesAClassPathWithoutTheGame() throws IOException {
        Path library = directory.resolve("library.jar");
        try (OutputStream file = Files.newOutputStream(library);
                ZipOutputStream archive = new ZipOutputStream(file)) {
            archive.putNextEntry(new ZipEntry(WINDOW_ENTRY));
            archive.closeEntry();
        }
        ReleasePlan plan = FixtureGame.plan(FixtureGame.SWAP_BUFFERS);
        FramePlan frame = framePlanOf(FixtureGame.UNPINNED_CLIENT);

        IllegalStateException refusal =
                assertThrows(IllegalStateException.class, () -> install(plan, frame, classPathOf(library)));

        assertTrue(refusal.getMessage().contains("no Minecraft client"), refusal.getMessage());
    }

    @Test
    void reportsAClassThatChangedAfterTheClientWasChecked() throws IOException, IllegalClassFormatException {
        byte[] window = pinnedWindow();
        Path client = clientHolding("client.jar", window);
        install(planPinnedTo(client), framePlanPinnedTo(client), classPathOf(client));

        byte[] changed = windowSwappingTwice();

        assertNull(transformers.get(0).transform(null, FixtureGame.WINDOW, null, null, changed));
        assertEquals(1, refusals.size());
    }

    private void install(ReleasePlan plan, FramePlan frame, String classPath) throws IOException {
        Instrumentation instrumentation = (Instrumentation) Proxy.newProxyInstance(
                Instrumentation.class.getClassLoader(),
                new Class<?>[] {Instrumentation.class},
                (proxy, method, arguments) -> {
                    if (!method.getName().equals("addTransformer")) {
                        throw new UnsupportedOperationException(method.getName());
                    }
                    transformers.add((ClassFileTransformer) arguments[0]);
                    return null;
                });
        ClientBootstrapAgent.install(plan, frame, classPath, instrumentation, refusals::add);
    }

    private Path clientHolding(String name, byte[] window) throws IOException {
        Path jar = directory.resolve(name);
        try (OutputStream file = Files.newOutputStream(jar);
                ZipOutputStream archive = new ZipOutputStream(file)) {
            archive.putNextEntry(new ZipEntry(MAIN_CLASS_ENTRY));
            archive.closeEntry();
            archive.putNextEntry(new ZipEntry(WINDOW_ENTRY));
            archive.write(window);
            archive.closeEntry();
        }
        return jar;
    }

    private String classPathOf(Path client) {
        return directory.resolve("absent.jar") + File.pathSeparator + client;
    }

    private static ReleasePlan planPinnedTo(Path client) throws IOException {
        String text = FixtureGame.planText(FixtureGame.SWAP_BUFFERS, sha1Of(client));
        return ReleasePlanFile.read(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
    }

    private static byte[] pinnedWindow() {
        return FixtureGame.windowCallingEach(FixtureGame.SWAP_BUFFERS, FixtureGame.MATRIX_MODE);
    }

    private static byte[] windowSwappingTwice() {
        return FixtureGame.windowCallingEach(
                FixtureGame.SWAP_BUFFERS, FixtureGame.SWAP_BUFFERS, FixtureGame.MATRIX_MODE);
    }

    private static FramePlan framePlanPinnedTo(Path client) throws IOException {
        return framePlanOf(sha1Of(client));
    }

    private static FramePlan framePlanOf(String clientSha1) throws IOException {
        String text = FixtureGame.framePlanText(FixtureGame.MATRIX_MODE, clientSha1, FixtureGame.STAND_IN);
        return FramePlan.read(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
    }

    private static String sha1Of(Path file) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
        try (InputStream content = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            for (int read = content.read(buffer); read != -1; read = content.read(buffer)) {
                digest.update(buffer, 0, read);
            }
        }
        StringBuilder hex = new StringBuilder();
        for (byte value : digest.digest()) {
            hex.append(String.format("%02x", value));
        }
        return hex.toString();
    }
}
