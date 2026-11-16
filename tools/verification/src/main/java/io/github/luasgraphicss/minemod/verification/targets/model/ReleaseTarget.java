package io.github.luasgraphicss.minemod.verification.targets.model;

import java.util.List;

public record ReleaseTarget(
        String id,
        String family,
        String releaseTime,
        PinnedDocument metadata,
        PinnedArtifact client,
        PinnedArtifact clientMappings,
        LaunchProfile launch,
        JavaRuntime runtime,
        LwjglBinding lwjgl,
        List<LoaderTarget> loaders) {

    public record PinnedDocument(String url, String sha1) {}

    public record PinnedArtifact(String url, String sha1, long size) {}

    public record LaunchProfile(String mainClass, List<String> gameArguments, List<String> jvmArguments) {}

    public record JavaRuntime(String component, int majorVersion) {}

    public record LwjglBinding(int generation, String version) {}
}
