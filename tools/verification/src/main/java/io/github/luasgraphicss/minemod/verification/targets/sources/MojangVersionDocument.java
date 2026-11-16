package io.github.luasgraphicss.minemod.verification.targets.sources;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.luasgraphicss.minemod.verification.targets.model.LoaderTarget;
import io.github.luasgraphicss.minemod.verification.targets.model.ReleaseTarget;
import io.github.luasgraphicss.minemod.verification.targets.model.ReleaseTarget.JavaRuntime;
import io.github.luasgraphicss.minemod.verification.targets.model.ReleaseTarget.LaunchProfile;
import io.github.luasgraphicss.minemod.verification.targets.model.ReleaseTarget.LwjglBinding;
import io.github.luasgraphicss.minemod.verification.targets.model.ReleaseTarget.PinnedArtifact;
import io.github.luasgraphicss.minemod.verification.targets.model.ReleaseTarget.PinnedDocument;
import io.github.luasgraphicss.minemod.verification.targets.policy.ReleaseCatalog.RequiredRelease;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class MojangVersionDocument {
    private static final List<String> SUPPORTED_SYSTEMS = List.of("windows", "linux");
    private static final String LWJGL_CORE_ARTIFACT = "lwjgl";
    private static final int COORDINATE_PARTS = 3;

    private MojangVersionDocument() {}

    static ReleaseTarget toReleaseTarget(
            JsonNode version,
            RequiredRelease required,
            String releaseTime,
            PinnedDocument metadata,
            List<LoaderTarget> loaders)
            throws MetadataFormatException {
        JsonNode downloads = JsonFields.require(version, "downloads");
        JsonNode javaVersion = JsonFields.require(version, "javaVersion");
        return new ReleaseTarget(
                required.id(),
                required.family(),
                releaseTime,
                metadata,
                artifact(JsonFields.require(downloads, "client")),
                downloads.has("client_mappings") ? artifact(downloads.get("client_mappings")) : null,
                launchProfile(version),
                new JavaRuntime(
                        JsonFields.text(javaVersion, "component"),
                        JsonFields.require(javaVersion, "majorVersion").asInt()),
                lwjglBinding(JsonFields.require(version, "libraries")),
                List.copyOf(loaders));
    }

    private static PinnedArtifact artifact(JsonNode download) throws MetadataFormatException {
        return new PinnedArtifact(
                JsonFields.text(download, "url"),
                JsonFields.text(download, "sha1"),
                JsonFields.require(download, "size").asLong());
    }

    private static LaunchProfile launchProfile(JsonNode version) throws MetadataFormatException {
        String mainClass = JsonFields.text(version, "mainClass");
        if (version.has("minecraftArguments")) {
            List<String> gameArguments = List.of(JsonFields.text(version, "minecraftArguments").split(" "));
            return new LaunchProfile(mainClass, gameArguments, List.of());
        }
        JsonNode arguments = JsonFields.require(version, "arguments");
        return new LaunchProfile(
                mainClass, unconditionalArguments(arguments.path("game")), unconditionalArguments(arguments.path("jvm")));
    }

    private static List<String> unconditionalArguments(JsonNode arguments) {
        List<String> unconditional = new ArrayList<>();
        for (JsonNode argument : arguments) {
            if (argument.isTextual()) {
                unconditional.add(argument.asText());
            }
        }
        return List.copyOf(unconditional);
    }

    private static LwjglBinding lwjglBinding(JsonNode libraries) throws MetadataFormatException {
        Set<LwjglBinding> bindings = new LinkedHashSet<>();
        for (JsonNode library : libraries) {
            String[] coordinate = JsonFields.text(library, "name").split(":");
            if (coordinate.length != COORDINATE_PARTS || !coordinate[1].equals(LWJGL_CORE_ARTIFACT)) {
                continue;
            }
            int generation = lwjglGeneration(coordinate[0]);
            boolean supported = SUPPORTED_SYSTEMS.stream().anyMatch(system -> appliesTo(library, system));
            if (generation != 0 && supported) {
                bindings.add(new LwjglBinding(generation, coordinate[2]));
            }
        }
        if (bindings.size() != 1) {
            throw new MetadataFormatException(
                    "expected exactly one LWJGL core binding for Windows and Linux, found " + bindings);
        }
        return bindings.iterator().next();
    }

    private static int lwjglGeneration(String group) {
        return switch (group) {
            case "org.lwjgl.lwjgl" -> 2;
            case "org.lwjgl" -> 3;
            default -> 0;
        };
    }

    private static boolean appliesTo(JsonNode library, String system) {
        JsonNode rules = library.get("rules");
        if (rules == null) {
            return true;
        }
        boolean allowed = false;
        for (JsonNode rule : rules) {
            JsonNode systemName = rule.path("os").path("name");
            if (systemName.isMissingNode() || systemName.asText().equals(system)) {
                allowed = rule.path("action").asText().equals("allow");
            }
        }
        return allowed;
    }
}
