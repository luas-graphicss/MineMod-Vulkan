package io.github.luasgraphicss.minemod.client.library;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

public final class BundledLibrary {
    private static final Set<PosixFilePermission> OWNER_ONLY = PosixFilePermissions.fromString("rwx------");
    private static final String POSIX_VIEW = "posix";
    private static final String PARTIAL_SUFFIX = ".partial";
    private static final int DIGEST_BLOCK_BYTES = 128;

    private final ClassLoader bundle;
    private final NativePlatform platform;

    public BundledLibrary(ClassLoader bundle, NativePlatform platform) {
        this.bundle = bundle;
        this.platform = platform;
    }

    public Path extractInto(Path cacheDirectory) {
        String declaredDigest = declaredDigest();
        Path directory = cacheDirectory.resolve(declaredDigest);
        Path library = directory.resolve(platform.libraryFile());
        try {
            if (Files.isRegularFile(library) && LibraryDigest.of(library).equals(declaredDigest)) {
                return library;
            }
            createPrivateDirectory(directory);
            extractVerified(directory, library, declaredDigest);
            return library;
        } catch (IOException failure) {
            throw new NativeLibraryUnavailable(
                    "extracting " + platform.libraryResource() + " into " + directory, failure);
        }
    }

    private String declaredDigest() {
        try (InputStream resource = open(platform.digestResource())) {
            ByteArrayOutputStream text = new ByteArrayOutputStream();
            byte[] block = new byte[DIGEST_BLOCK_BYTES];
            for (int read = resource.read(block); read >= 0; read = resource.read(block)) {
                text.write(block, 0, read);
            }
            String digest = new String(text.toByteArray(), StandardCharsets.US_ASCII).trim();
            if (!LibraryDigest.isWellFormed(digest)) {
                throw new NativeLibraryUnavailable(platform.digestResource() + " does not hold a SHA-256 digest");
            }
            return digest;
        } catch (IOException failure) {
            throw new NativeLibraryUnavailable("reading " + platform.digestResource(), failure);
        }
    }

    private void extractVerified(Path directory, Path library, String declaredDigest) throws IOException {
        Path staged = Files.createTempFile(directory, platform.libraryFile(), PARTIAL_SUFFIX);
        try (InputStream resource = open(platform.libraryResource())) {
            String extractedDigest = LibraryDigest.copy(resource, staged);
            if (!extractedDigest.equals(declaredDigest)) {
                throw new NativeLibraryUnavailable(platform.libraryResource() + " has digest " + extractedDigest
                        + " and the bundle declares " + declaredDigest);
            }
            Files.move(staged, library, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(staged);
        }
    }

    private InputStream open(String resource) {
        InputStream content = bundle.getResourceAsStream(resource);
        if (content == null) {
            throw new NativeLibraryUnavailable("this build bundles no " + resource);
        }
        return content;
    }

    private static void createPrivateDirectory(Path directory) throws IOException {
        Files.createDirectories(directory);
        if (directory.getFileSystem().supportedFileAttributeViews().contains(POSIX_VIEW)) {
            Files.setPosixFilePermissions(directory, OWNER_ONLY);
        }
    }
}
