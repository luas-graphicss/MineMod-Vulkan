package io.github.luasgraphicss.minemod.verification.scenario.bootstrap.install;

import java.util.List;

public record LauncherProfile(
        String id,
        String inheritsFrom,
        String type,
        String releaseTime,
        String time,
        String mainClass,
        Arguments arguments,
        List<Library> libraries) {

    public record Arguments(List<String> game, List<String> jvm) {}

    public record Library(String name, Downloads downloads) {}

    public record Downloads(Artifact artifact) {}

    public record Artifact(String path, String url, String sha1, long size) {}
}
