package io.github.luasgraphicss.minemod.compatibility.provider.load;

import io.github.luasgraphicss.minemod.compatibility.provider.artifact.ArtifactDigest;
import io.github.luasgraphicss.minemod.compatibility.provider.artifact.ProviderPin;
import io.github.luasgraphicss.minemod.compatibility.provider.load.ProviderLoadException.Reason;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Objects;
import java.util.Set;

public final class IsolatedProviderLoader {
    private static final String POSIX_VIEW = "posix";

    private final Path privateDirectory;
    private final ProviderPin pin;
    private final LibraryOpener opener;

    public IsolatedProviderLoader(Path privateDirectory, ProviderPin pin, LibraryOpener opener) throws IOException {
        this.privateDirectory = privateDirectory.toRealPath();
        this.pin = Objects.requireNonNull(pin, "pin");
        this.opener = Objects.requireNonNull(opener, "opener");
    }

    public LoadedProvider load(String libraryFileName) throws IOException, ProviderLoadException {
        requirePrivateDirectory();
        Path library = locate(libraryFileName);
        if (!ArtifactDigest.ofFile(library).equals(pin.libraryDigest())) {
            throw new ProviderLoadException(
                    Reason.DIGEST_MISMATCH,
                    libraryFileName + " is not the pinned Mesa " + pin.mesaVersion() + " build");
        }
        long handle = opener.open(library);
        if (handle == LibraryOpener.FAILED) {
            throw new ProviderLoadException(Reason.OPEN_FAILED, libraryFileName + " could not be opened");
        }
        return new LoadedProvider(handle, libraryFileName, opener);
    }

    private void requirePrivateDirectory() throws IOException, ProviderLoadException {
        if (!privateDirectory.getFileSystem().supportedFileAttributeViews().contains(POSIX_VIEW)) {
            return;
        }
        Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(privateDirectory);
        if (permissions.contains(PosixFilePermission.GROUP_WRITE)
                || permissions.contains(PosixFilePermission.OTHERS_WRITE)) {
            throw new ProviderLoadException(
                    Reason.DIRECTORY_WRITABLE_BY_OTHERS, "the provider directory can be written by other users");
        }
    }

    private Path locate(String libraryFileName) throws ProviderLoadException {
        Path name = Paths.get(libraryFileName);
        if (name.isAbsolute() || name.getNameCount() != 1) {
            throw new ProviderLoadException(
                    Reason.NOT_A_FILE_NAME, "the provider is loaded by file name only: " + libraryFileName);
        }
        Path library = privateDirectory.resolve(name);
        if (!Files.exists(library, LinkOption.NOFOLLOW_LINKS)) {
            throw new ProviderLoadException(Reason.MISSING, libraryFileName + " is not in the provider directory");
        }
        if (!Files.isRegularFile(library, LinkOption.NOFOLLOW_LINKS)) {
            throw new ProviderLoadException(Reason.NOT_A_REGULAR_FILE, libraryFileName + " is not a regular file");
        }
        return library;
    }
}
