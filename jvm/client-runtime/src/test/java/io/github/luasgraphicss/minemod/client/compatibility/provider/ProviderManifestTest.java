package io.github.luasgraphicss.minemod.client.compatibility.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.client.compatibility.ModOpenGlUnavailable;
import io.github.luasgraphicss.minemod.compatibility.runtime.provider.ProviderBundle;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProviderManifestTest {
    private static final int DIGEST_DIGITS = 64;
    private static final String ENTRY = "libEGL.so.1";
    private static final String DISPATCH = "libGLdispatch.so.0";
    private static final String DRIVERS = "libgallium-24.2.6.so";
    private static final String MESA_LINE = "mesa=24.2.6";
    private static final String ENTRY_LINE = "entry=" + ENTRY;
    private static final String ENTRY_DIGEST_LINE = "sha256." + ENTRY + "=" + digestOf('a');

    @TempDir
    Path directory;

    @Test
    void theBundleLoadsItsDependenciesInTheOrderTheManifestListsThem() throws IOException {
        write(
                MESA_LINE,
                ENTRY_LINE,
                "dependencies=" + DISPATCH + " " + DRIVERS,
                ENTRY_DIGEST_LINE,
                "sha256." + DRIVERS + "=" + digestOf('c'),
                "sha256." + DISPATCH + "=" + digestOf('b'),
                "driver.MESA_LOADER_DRIVER_OVERRIDE=zink",
                "driver.GALLIUM_DRIVER=zink");

        ProviderBundle bundle = ProviderManifest.read(directory);

        assertEquals(directory, bundle.directory());
        assertEquals(ENTRY, bundle.entryLibrary());
        assertEquals("24.2.6", bundle.pin().mesaVersion());
        assertEquals(digestOf('a'), bundle.pin().libraryDigest().hex());
        assertEquals(Arrays.asList(DISPATCH, DRIVERS), new ArrayList<>(bundle.dependencies().keySet()));
        assertEquals(digestOf('c'), bundle.dependencies().get(DRIVERS).hex());
        assertEquals(
                Arrays.asList("GALLIUM_DRIVER", "MESA_LOADER_DRIVER_OVERRIDE"),
                new ArrayList<>(bundle.driverSettings().keySet()));
        assertEquals("zink", bundle.driverSettings().get("GALLIUM_DRIVER"));
    }

    @Test
    void aBundleWithoutDependenciesLoadsItsEntryAlone() throws IOException {
        write(MESA_LINE, ENTRY_LINE, ENTRY_DIGEST_LINE);

        ProviderBundle bundle = ProviderManifest.read(directory);

        assertEquals(Collections.emptyMap(), bundle.dependencies());
        assertEquals(Collections.emptyMap(), bundle.driverSettings());
    }

    @Test
    void aDirectoryWithoutAManifestIsRefused() {
        ModOpenGlUnavailable refusal = assertThrows(ModOpenGlUnavailable.class, () -> ProviderManifest.read(directory));

        assertTrue(refusal.getMessage().startsWith("reading the provider manifest "), refusal.getMessage());
    }

    @Test
    void aManifestThatNamesNoMesaReleaseIsRefused() throws IOException {
        write(ENTRY_LINE, ENTRY_DIGEST_LINE);

        assertRefusedFor("holds no mesa");
    }

    @Test
    void aLibraryWithoutADigestIsRefused() throws IOException {
        write(MESA_LINE, ENTRY_LINE, "dependencies=" + DISPATCH, ENTRY_DIGEST_LINE);

        assertRefusedFor("holds no sha256." + DISPATCH);
    }

    @Test
    void aDigestThatIsNotSha256IsRefused() throws IOException {
        write(MESA_LINE, ENTRY_LINE, "sha256." + ENTRY + "=ABCDEF");

        ModOpenGlUnavailable refusal = assertThrows(ModOpenGlUnavailable.class, () -> ProviderManifest.read(directory));

        assertTrue(refusal.getMessage().startsWith("reading the digest of " + ENTRY), refusal.getMessage());
    }

    @Test
    void aLibraryNamedByAPathIsRefused() throws IOException {
        write(MESA_LINE, "entry=../" + ENTRY, "sha256.../" + ENTRY + "=" + digestOf('a'));

        assertRefusedFor("names ../" + ENTRY + ", which is not the file name of a library");
    }

    @Test
    void aDependencyListedTwiceIsRefused() throws IOException {
        write(
                MESA_LINE,
                ENTRY_LINE,
                "dependencies=" + DISPATCH + " " + DISPATCH,
                ENTRY_DIGEST_LINE,
                "sha256." + DISPATCH + "=" + digestOf('b'));

        assertRefusedFor("lists " + DISPATCH + " twice");
    }

    @Test
    void aDigestOfALibraryTheProviderDoesNotLoadIsRefused() throws IOException {
        write(MESA_LINE, ENTRY_LINE, ENTRY_DIGEST_LINE, "sha256." + DRIVERS + "=" + digestOf('c'));

        assertRefusedFor("holds sha256." + DRIVERS + ", which describes nothing the provider loads");
    }

    @Test
    void aDriverSettingThatIsNotAVariableNameIsRefused() throws IOException {
        write(MESA_LINE, ENTRY_LINE, ENTRY_DIGEST_LINE, "driver.gallium-driver=zink");

        assertRefusedFor("sets gallium-driver, which is not the name of a driver setting");
    }

    private void assertRefusedFor(String defect) {
        ModOpenGlUnavailable refusal = assertThrows(ModOpenGlUnavailable.class, () -> ProviderManifest.read(directory));

        assertTrue(refusal.getMessage().endsWith(defect), refusal.getMessage());
    }

    private void write(String... lines) throws IOException {
        Files.write(directory.resolve(ProviderManifest.FILE_NAME), Arrays.asList(lines), StandardCharsets.UTF_8);
    }

    private static String digestOf(char digit) {
        char[] digits = new char[DIGEST_DIGITS];
        Arrays.fill(digits, digit);
        return new String(digits);
    }
}
