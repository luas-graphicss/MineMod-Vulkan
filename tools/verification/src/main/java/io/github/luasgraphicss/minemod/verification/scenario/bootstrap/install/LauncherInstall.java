package io.github.luasgraphicss.minemod.verification.scenario.bootstrap.install;

import io.github.luasgraphicss.minemod.verification.json.RepositoryJson;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public record LauncherInstall(Path directory, Path profileFile, LauncherProfile profile) {
    public static final String LIBRARY_DIRECTORY = "${library_directory}";
    public static final String AGENT_OPTION = "-javaagent:";

    private static final String BUILT_INSTALL = "build/install";
    private static final String VERSIONS = "versions";
    private static final String LIBRARIES = "libraries";
    private static final String PROFILE_SUFFIX = ".json";
    private static final long MAX_PROFILE_BYTES = 64L * 1024;

    public static Optional<LauncherInstall> builtBy(Path repositoryRoot, String module) throws IOException {
        Path directory = repositoryRoot.resolve(module).resolve(BUILT_INSTALL);
        if (!Files.isDirectory(directory.resolve(VERSIONS))) {
            return Optional.empty();
        }
        List<Path> profiles;
        try (Stream<Path> files = Files.walk(directory.resolve(VERSIONS))) {
            profiles = files.filter(file -> file.getFileName().toString().endsWith(PROFILE_SUFFIX))
                    .sorted()
                    .toList();
        }
        if (profiles.size() != 1) {
            throw new IOException(directory + " holds " + profiles.size() + " launcher profiles, expected one");
        }
        Path profileFile = profiles.getFirst();
        return Optional.of(new LauncherInstall(
                directory, profileFile, RepositoryJson.read(profileFile, LauncherProfile.class, MAX_PROFILE_BYTES)));
    }

    public List<String> jvmArguments() {
        String libraries = directory.resolve(LIBRARIES).toString();
        return profile.arguments().jvm().stream()
                .map(argument -> argument.replace(LIBRARY_DIRECTORY, libraries))
                .toList();
    }

    public List<Path> agents() {
        return jvmArguments().stream()
                .filter(argument -> argument.startsWith(AGENT_OPTION))
                .map(argument -> Path.of(argument.substring(AGENT_OPTION.length())))
                .toList();
    }

    public List<Path> files() throws IOException {
        try (Stream<Path> files = Files.walk(directory)) {
            return files.filter(Files::isRegularFile).sorted().toList();
        }
    }
}
