package io.github.luasgraphicss.minemod.verification.adapters.source;

import io.github.luasgraphicss.minemod.verification.hooks.cache.PinnedDownloads;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Set;

public final class NameDownloads implements AutoCloseable {
    private static final Duration TIMEOUT = Duration.ofMinutes(2);
    private static final int HTTP_OK = 200;
    private static final int MAX_BYTES = 16 * 1024 * 1024;
    private static final Set<String> HOSTS = Set.of("maven.fabricmc.net", "maven.minecraftforge.net");
    private static final String DIRECTORY = "names";
    private static final String STAGED_SUFFIX = ".staged";

    private final Path directory;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    public NameDownloads(Path cacheDirectory) {
        this.directory = cacheDirectory.resolve(DIRECTORY);
    }

    public Path fetch(NameSource source) throws IOException, InterruptedException {
        Path cached = cachedFileOf(source);
        if (!Files.isRegularFile(cached)) {
            store(source, cached);
        }
        return cached;
    }

    public Path fetchPinned(NameSource source, String sha1) throws IOException, InterruptedException {
        Path cached = cachedFileOf(source);
        if (!Files.isRegularFile(cached) || !PinnedDownloads.sha1Of(cached).equals(sha1)) {
            store(source, cached);
        }
        String found = PinnedDownloads.sha1Of(cached);
        if (!found.equals(sha1)) {
            throw new IOException(source.url() + " has SHA-1 " + found + ", the adapter pins " + sha1);
        }
        return cached;
    }

    @Override
    public void close() {
        http.close();
    }

    private Path cachedFileOf(NameSource source) throws IOException {
        URI location = URI.create(source.url());
        if (!"https".equals(location.getScheme()) || !HOSTS.contains(location.getHost())) {
            throw new IOException("refusing to download names outside " + HOSTS + ": " + location);
        }
        String path = location.getPath();
        return directory.resolve(path.substring(path.lastIndexOf('/') + 1));
    }

    private void store(NameSource source, Path cached) throws IOException, InterruptedException {
        Files.createDirectories(directory);
        Path staged = cached.resolveSibling(cached.getFileName() + STAGED_SUFFIX);
        try {
            Files.write(staged, download(URI.create(source.url())));
            Files.move(staged, cached, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(staged);
        }
    }

    private byte[] download(URI location) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(location).timeout(TIMEOUT).GET().build();
        HttpResponse<InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
        try (InputStream body = response.body()) {
            if (response.statusCode() != HTTP_OK) {
                throw new IOException(location + " answered HTTP " + response.statusCode());
            }
            byte[] content = body.readNBytes(MAX_BYTES + 1);
            if (content.length > MAX_BYTES) {
                throw new IOException(location + " exceeds " + MAX_BYTES + " bytes");
            }
            return content;
        }
    }
}
