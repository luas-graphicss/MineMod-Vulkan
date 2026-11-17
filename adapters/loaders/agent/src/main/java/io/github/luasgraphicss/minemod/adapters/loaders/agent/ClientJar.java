package io.github.luasgraphicss.minemod.adapters.loaders.agent;

import io.github.luasgraphicss.minemod.adapters.transformers.preflight.ClassSource;
import io.github.luasgraphicss.minemod.adapters.transformers.release.PinnedReleases;
import io.github.luasgraphicss.minemod.adapters.transformers.release.ReleasePlan;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

final class ClientJar {
    private static final String MAIN_CLASS_ENTRY = "net/minecraft/client/main/Main.class";
    private static final String ARCHIVE_SUFFIX = ".jar";
    private static final String CLASS_SUFFIX = ".class";
    private static final int BUFFER_BYTES = 64 * 1024;

    private final Path jar;

    private ClientJar(Path jar) {
        this.jar = jar;
    }

    static ClientJar onClassPath(String classPath) throws IOException {
        List<Path> clients = new ArrayList<>();
        for (String entry : classPath.split(Pattern.quote(File.pathSeparator))) {
            Path candidate = Paths.get(entry);
            if (entry.endsWith(ARCHIVE_SUFFIX) && Files.isRegularFile(candidate) && holdsTheGame(candidate)) {
                clients.add(candidate);
            }
        }
        if (clients.isEmpty()) {
            throw new IllegalStateException("the class path holds no Minecraft client; the game was not started");
        }
        if (clients.size() > 1) {
            throw new IllegalStateException("the class path holds more than one Minecraft client: " + clients);
        }
        return new ClientJar(clients.get(0));
    }

    void requirePinnedBy(ReleasePlan plan) throws IOException {
        new PinnedReleases(Collections.singletonList(plan)).requirePlanFor(sha1());
    }

    Classes openClasses() throws IOException {
        return new Classes(new ZipFile(jar.toFile()));
    }

    private String sha1() throws IOException {
        MessageDigest digest = sha1Digest();
        byte[] buffer = new byte[BUFFER_BYTES];
        try (InputStream content = Files.newInputStream(jar)) {
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

    private static boolean holdsTheGame(Path archive) throws IOException {
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            return zip.getEntry(MAIN_CLASS_ENTRY) != null;
        } catch (ZipException notAnArchive) {
            return false;
        }
    }

    private static MessageDigest sha1Digest() {
        try {
            return MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("every Java runtime provides SHA-1", impossible);
        }
    }

    static final class Classes implements ClassSource, Closeable {
        private final ZipFile archive;

        private Classes(ZipFile archive) {
            this.archive = archive;
        }

        @Override
        public Optional<byte[]> classFile(String internalName) throws IOException {
            ZipEntry entry = archive.getEntry(internalName + CLASS_SUFFIX);
            if (entry == null) {
                return Optional.empty();
            }
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[BUFFER_BYTES];
            try (InputStream content = archive.getInputStream(entry)) {
                for (int read = content.read(buffer); read != -1; read = content.read(buffer)) {
                    bytes.write(buffer, 0, read);
                }
            }
            return Optional.of(bytes.toByteArray());
        }

        @Override
        public void close() throws IOException {
            archive.close();
        }
    }
}
