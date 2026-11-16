package io.github.luasgraphicss.minemod.client.library;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BundledLibraryTest {
    private static final NativePlatform PLATFORM = NativePlatform.of("Linux", "amd64");
    private static final byte[] LIBRARY = "the native renderer".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] ALTERED_LIBRARY = "something else".getBytes(StandardCharsets.US_ASCII);

    @TempDir
    Path workspace;

    @Test
    void extractsTheLibraryUnderItsDeclaredDigest() throws IOException {
        Path library = extract(bundleOf(LIBRARY, sha256(LIBRARY)));

        assertEquals(cache().resolve(sha256(LIBRARY)).resolve("libminemod_jni.so"), library);
        assertArrayEquals(LIBRARY, Files.readAllBytes(library));
    }

    @Test
    void reusesALibraryThatIsAlreadyExtracted() throws IOException {
        Path bundle = bundleOf(LIBRARY, sha256(LIBRARY));
        Path first = extract(bundle);
        Files.delete(bundle.resolve(PLATFORM.libraryResource()));

        assertEquals(first, extract(bundle));
    }

    @Test
    void replacesAnExtractedLibraryThatWasAltered() throws IOException {
        Path bundle = bundleOf(LIBRARY, sha256(LIBRARY));
        Files.write(extract(bundle), ALTERED_LIBRARY);

        assertArrayEquals(LIBRARY, Files.readAllBytes(extract(bundle)));
    }

    @Test
    void refusesALibraryThatDoesNotMatchItsDeclaredDigest() throws IOException {
        Path bundle = bundleOf(ALTERED_LIBRARY, sha256(LIBRARY));

        assertThrows(NativeLibraryUnavailable.class, () -> extract(bundle));
        assertFalse(Files.exists(cache().resolve(sha256(LIBRARY)).resolve("libminemod_jni.so")));
    }

    @Test
    void refusesADeclarationThatIsNotADigest() throws IOException {
        Path bundle = bundleOf(LIBRARY, "../../outside");

        assertThrows(NativeLibraryUnavailable.class, () -> extract(bundle));
        assertFalse(Files.exists(cache()));
    }

    @Test
    void refusesABuildWithoutTheLibraryOfThePlatform() throws IOException {
        Path emptyBundle = Files.createDirectories(workspace.resolve("bundle"));

        assertThrows(NativeLibraryUnavailable.class, () -> extract(emptyBundle));
    }

    private Path extract(Path bundle) throws IOException {
        try (URLClassLoader resources = new URLClassLoader(new URL[] {bundle.toUri().toURL()}, null)) {
            return new BundledLibrary(resources, PLATFORM).extractInto(cache());
        }
    }

    private Path cache() {
        return workspace.resolve("cache");
    }

    private Path bundleOf(byte[] library, String declaredDigest) throws IOException {
        Path bundle = workspace.resolve("bundle");
        Path bundledLibrary = bundle.resolve(PLATFORM.libraryResource());
        Files.createDirectories(bundledLibrary.getParent());
        Files.write(bundledLibrary, library);
        Files.write(bundle.resolve(PLATFORM.digestResource()), declaredDigest.getBytes(StandardCharsets.US_ASCII));
        return bundle;
    }

    private static String sha256(byte[] content) {
        try {
            StringBuilder text = new StringBuilder();
            for (byte value : MessageDigest.getInstance("SHA-256").digest(content)) {
                text.append(String.format("%02x", value));
            }
            return text.toString();
        } catch (NoSuchAlgorithmException missing) {
            throw new IllegalStateException(missing);
        }
    }
}
