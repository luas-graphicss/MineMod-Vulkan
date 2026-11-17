package io.github.luasgraphicss.minemod.adapters.presentation.renderer.fixtures;

import io.github.luasgraphicss.minemod.client.library.NativePlatform;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class RendererBundle {
    private RendererBundle() {}

    public static ClassLoader around(Path builtLibrary, Path bundleRoot) throws IOException {
        NativePlatform platform = NativePlatform.current();
        Path library = bundleRoot.resolve(platform.libraryResource());
        Files.createDirectories(library.getParent());
        Files.copy(builtLibrary, library);
        Files.write(
                bundleRoot.resolve(platform.digestResource()),
                sha256Of(library).getBytes(StandardCharsets.US_ASCII));
        return new URLClassLoader(new URL[] {bundleRoot.toUri().toURL()}, null);
    }

    private static String sha256Of(Path file) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("every Java runtime provides SHA-256", impossible);
        }
        StringBuilder hex = new StringBuilder();
        for (byte value : digest.digest(Files.readAllBytes(file))) {
            hex.append(String.format("%02x", value));
        }
        return hex.toString();
    }
}
