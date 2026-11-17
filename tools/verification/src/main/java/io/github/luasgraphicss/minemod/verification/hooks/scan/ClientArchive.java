package io.github.luasgraphicss.minemod.verification.hooks.scan;

import io.github.luasgraphicss.minemod.adapters.transformers.preflight.ClassSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class ClientArchive implements ClassSource, AutoCloseable {
    private static final String CLASS_SUFFIX = ".class";
    private static final int MAX_CLASS_BYTES = 8 * 1024 * 1024;

    private final ZipFile archive;

    private ClientArchive(ZipFile archive) {
        this.archive = archive;
    }

    public static ClientArchive open(Path clientJar) throws IOException {
        return new ClientArchive(new ZipFile(clientJar.toFile()));
    }

    public List<String> classNames() {
        return archive.stream()
                .map(ZipEntry::getName)
                .filter(name -> name.endsWith(CLASS_SUFFIX))
                .map(name -> name.substring(0, name.length() - CLASS_SUFFIX.length()))
                .sorted()
                .toList();
    }

    @Override
    public Optional<byte[]> classFile(String internalName) throws IOException {
        ZipEntry entry = archive.getEntry(internalName + CLASS_SUFFIX);
        if (entry == null) {
            return Optional.empty();
        }
        try (InputStream content = archive.getInputStream(entry)) {
            byte[] bytes = content.readNBytes(MAX_CLASS_BYTES + 1);
            if (bytes.length > MAX_CLASS_BYTES) {
                throw new IOException(internalName + " exceeds " + MAX_CLASS_BYTES + " bytes");
            }
            return Optional.of(bytes);
        }
    }

    @Override
    public void close() throws IOException {
        archive.close();
    }
}
