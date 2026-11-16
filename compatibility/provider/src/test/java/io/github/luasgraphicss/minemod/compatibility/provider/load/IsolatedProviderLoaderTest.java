package io.github.luasgraphicss.minemod.compatibility.provider.load;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.github.luasgraphicss.minemod.compatibility.provider.artifact.ArtifactDigest;
import io.github.luasgraphicss.minemod.compatibility.provider.artifact.ProviderPin;
import io.github.luasgraphicss.minemod.compatibility.provider.load.ProviderLoadException.Reason;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class IsolatedProviderLoaderTest {
    private static final String LIBRARY = "libGL.so.1";
    private static final byte[] PINNED_BUILD = "pinned build".getBytes(StandardCharsets.UTF_8);

    @TempDir
    Path directory;

    @TempDir
    Path elsewhere;

    private final RecordingOpener opener = new RecordingOpener();
    private IsolatedProviderLoader loader;

    @BeforeEach
    void pinTheBuild() throws IOException {
        Path reference = Files.write(elsewhere.resolve("reference"), PINNED_BUILD);
        ProviderPin pin = new ProviderPin("25.0.7", ArtifactDigest.ofFile(reference));
        loader = new IsolatedProviderLoader(directory, pin, opener);
    }

    @Test
    void thePinnedBuildIsOpenedFromTheProviderDirectory() throws Exception {
        Files.write(directory.resolve(LIBRARY), PINNED_BUILD);

        LoadedProvider provider = loader.load(LIBRARY);

        assertEquals(RecordingOpener.HANDLE, provider.handle());
        assertEquals(Collections.singletonList(directory.toRealPath().resolve(LIBRARY)), opener.opened());
    }

    @Test
    void aBuildThatDiffersFromThePinIsNeverOpened() throws Exception {
        Files.write(directory.resolve(LIBRARY), "another build".getBytes(StandardCharsets.UTF_8));

        assertEquals(Reason.DIGEST_MISMATCH, failureOf(LIBRARY));
        assertEquals(Collections.emptyList(), opener.opened());
    }

    @Test
    void aPathIsNotAccepted() throws Exception {
        Path outside = Files.write(elsewhere.resolve(LIBRARY), PINNED_BUILD);

        assertEquals(Reason.NOT_A_FILE_NAME, failureOf(outside.toString()));
        assertEquals(Reason.NOT_A_FILE_NAME, failureOf("nested/" + LIBRARY));
        assertEquals(Reason.NOT_A_REGULAR_FILE, failureOf(".."));
    }

    @Test
    void aLinkToALibraryElsewhereIsRefused() throws Exception {
        Path outside = Files.write(elsewhere.resolve(LIBRARY), PINNED_BUILD);
        try {
            Files.createSymbolicLink(directory.resolve(LIBRARY), outside);
        } catch (UnsupportedOperationException | IOException exception) {
            assumeTrue(false, "symbolic links are not available here");
        }

        assertEquals(Reason.NOT_A_REGULAR_FILE, failureOf(LIBRARY));
    }

    @Test
    void aDirectoryThatOtherUsersCanWriteIsRefused() throws Exception {
        assumeTrue(directory.getFileSystem().supportedFileAttributeViews().contains("posix"));
        Files.write(directory.resolve(LIBRARY), PINNED_BUILD);
        Files.setPosixFilePermissions(directory, PosixFilePermissions.fromString("rwxrwxrwx"));

        assertEquals(Reason.DIRECTORY_WRITABLE_BY_OTHERS, failureOf(LIBRARY));
        assertEquals(Collections.emptyList(), opener.opened());
    }

    @Test
    void aMissingLibraryAndARefusedOpenAreToldApart() throws Exception {
        assertEquals(Reason.MISSING, failureOf(LIBRARY));

        Files.write(directory.resolve(LIBRARY), PINNED_BUILD);
        opener.refuse();

        assertEquals(Reason.OPEN_FAILED, failureOf(LIBRARY));
    }

    @Test
    void unloadingClosesTheLibraryOnce() throws Exception {
        Files.write(directory.resolve(LIBRARY), PINNED_BUILD);
        LoadedProvider provider = loader.load(LIBRARY);

        provider.close();
        provider.close();

        assertEquals(Collections.singletonList(RecordingOpener.HANDLE), opener.closed());
        assertThrows(IllegalStateException.class, provider::handle);
    }

    private Reason failureOf(String libraryFileName) {
        return assertThrows(ProviderLoadException.class, () -> loader.load(libraryFileName)).reason();
    }
}
