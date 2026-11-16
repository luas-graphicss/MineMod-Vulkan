package io.github.luasgraphicss.minemod.verification.targets.sources;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.luasgraphicss.minemod.verification.targets.model.LoaderTarget;
import io.github.luasgraphicss.minemod.verification.targets.model.ReleaseTarget;
import io.github.luasgraphicss.minemod.verification.targets.model.ReleaseTarget.PinnedDocument;
import io.github.luasgraphicss.minemod.verification.targets.policy.ReleaseCatalog.RequiredRelease;
import java.io.IOException;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

public final class MojangReleaseSource {
    public static final URI VERSION_MANIFEST =
            URI.create("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json");

    private final MetadataClient client;
    private final Map<String, JsonNode> entriesById;

    private MojangReleaseSource(MetadataClient client, Map<String, JsonNode> entriesById) {
        this.client = client;
        this.entriesById = entriesById;
    }

    public static MojangReleaseSource load(MetadataClient client) throws IOException, InterruptedException {
        Map<String, JsonNode> entriesById = new HashMap<>();
        for (JsonNode entry : JsonFields.require(client.fetch(VERSION_MANIFEST), "versions")) {
            entriesById.put(JsonFields.text(entry, "id"), entry);
        }
        return new MojangReleaseSource(client, entriesById);
    }

    public ReleaseTarget release(RequiredRelease required, List<LoaderTarget> loaders)
            throws IOException, InterruptedException {
        JsonNode entry = entriesById.get(required.id());
        if (entry == null || !JsonFields.text(entry, "type").equals("release")) {
            throw new MetadataFormatException("Mojang lists no stable release " + required.id());
        }
        PinnedDocument metadata = new PinnedDocument(JsonFields.text(entry, "url"), JsonFields.text(entry, "sha1"));
        byte[] document = client.fetchBytes(URI.create(metadata.url()));
        if (!sha1(document).equals(metadata.sha1())) {
            throw new IOException("version metadata of " + required.id() + " does not match its SHA-1 pin");
        }
        return MojangVersionDocument.toReleaseTarget(
                client.parse(document), required, JsonFields.text(entry, "releaseTime"), metadata, loaders);
    }

    private static String sha1(byte[] document) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(document));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("every Java runtime must provide SHA-1", exception);
        }
    }
}
