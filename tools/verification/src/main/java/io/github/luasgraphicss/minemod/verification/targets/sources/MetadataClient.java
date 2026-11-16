package io.github.luasgraphicss.minemod.verification.targets.sources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;

public final class MetadataClient implements AutoCloseable {
    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private static final int MAX_DOCUMENT_BYTES = 16 * 1024 * 1024;
    private static final int HTTP_OK = 200;
    private static final Set<String> OFFICIAL_HOSTS = Set.of(
            "piston-meta.mojang.com", "meta.fabricmc.net", "files.minecraftforge.net", "maven.neoforged.net");

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    public JsonNode fetch(URI document) throws IOException, InterruptedException {
        return parse(fetchBytes(document));
    }

    public JsonNode parse(byte[] document) throws IOException {
        return mapper.readTree(document);
    }

    public byte[] fetchBytes(URI document) throws IOException, InterruptedException {
        if (!"https".equals(document.getScheme()) || !OFFICIAL_HOSTS.contains(document.getHost())) {
            throw new IOException("refusing to fetch outside the official metadata hosts: " + document);
        }
        HttpRequest request = HttpRequest.newBuilder(document)
                .timeout(TIMEOUT)
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
        try (InputStream body = response.body()) {
            if (response.statusCode() != HTTP_OK) {
                throw new IOException(document + " answered HTTP " + response.statusCode());
            }
            byte[] bytes = body.readNBytes(MAX_DOCUMENT_BYTES + 1);
            if (bytes.length > MAX_DOCUMENT_BYTES) {
                throw new IOException(document + " exceeds " + MAX_DOCUMENT_BYTES + " bytes");
            }
            return bytes;
        }
    }

    @Override
    public void close() {
        http.close();
    }
}
