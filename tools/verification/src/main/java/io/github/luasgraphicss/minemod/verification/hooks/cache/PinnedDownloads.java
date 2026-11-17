package io.github.luasgraphicss.minemod.verification.hooks.cache;

import io.github.luasgraphicss.minemod.verification.targets.model.ReleaseTarget;
import io.github.luasgraphicss.minemod.verification.targets.model.ReleaseTarget.PinnedArtifact;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;

public final class PinnedDownloads implements AutoCloseable {
    private static final Duration TIMEOUT = Duration.ofMinutes(2);
    private static final int HTTP_OK = 200;
    private static final int BUFFER_BYTES = 64 * 1024;
    private static final String OFFICIAL_HOST_SUFFIX = ".mojang.com";
    private static final String CLIENTS = "clients";
    private static final String MAPPINGS = "mappings";
    private static final String STAGED_SUFFIX = ".staged";

    private final Path directory;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    public PinnedDownloads(Path directory) {
        this.directory = directory;
    }

    public Path clientOf(ReleaseTarget release) throws IOException, InterruptedException {
        return fetch(release.client(), directory.resolve(CLIENTS).resolve(release.id() + ".jar"));
    }

    public Optional<Path> clientMappingsOf(ReleaseTarget release) throws IOException, InterruptedException {
        if (release.clientMappings() == null) {
            return Optional.empty();
        }
        return Optional.of(fetch(release.clientMappings(), directory.resolve(MAPPINGS).resolve(release.id() + ".txt")));
    }

    public static String sha1Of(Path file) throws IOException {
        MessageDigest digest = sha1();
        byte[] buffer = new byte[BUFFER_BYTES];
        try (InputStream content = Files.newInputStream(file)) {
            for (int read = content.read(buffer); read != -1; read = content.read(buffer)) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private Path fetch(PinnedArtifact artifact, Path cached) throws IOException, InterruptedException {
        if (Files.isRegularFile(cached) && sha1Of(cached).equals(artifact.sha1())) {
            return cached;
        }
        URI location = URI.create(artifact.url());
        if (!"https".equals(location.getScheme())
                || location.getHost() == null
                || !location.getHost().endsWith(OFFICIAL_HOST_SUFFIX)) {
            throw new IOException("refusing to download outside the official Mojang hosts: " + location);
        }
        Files.createDirectories(cached.getParent());
        Path staged = cached.resolveSibling(cached.getFileName() + STAGED_SUFFIX);
        try {
            download(location, staged, artifact.size());
            String sha1 = sha1Of(staged);
            if (!sha1.equals(artifact.sha1())) {
                throw new IOException(location + " has SHA-1 " + sha1 + ", the manifest pins " + artifact.sha1());
            }
            Files.move(staged, cached, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(staged);
        }
        return cached;
    }

    private void download(URI location, Path target, long pinnedSize) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(location).timeout(TIMEOUT).GET().build();
        HttpResponse<InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
        try (InputStream body = response.body()) {
            if (response.statusCode() != HTTP_OK) {
                throw new IOException(location + " answered HTTP " + response.statusCode());
            }
            long written = Files.copy(new BoundedInputStream(body, pinnedSize), target, StandardCopyOption.REPLACE_EXISTING);
            if (written != pinnedSize) {
                throw new IOException(location + " has " + written + " bytes, the manifest pins " + pinnedSize);
            }
        }
    }

    private static MessageDigest sha1() {
        try {
            return MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("every Java runtime provides SHA-1", exception);
        }
    }

    @Override
    public void close() {
        http.close();
    }
}
