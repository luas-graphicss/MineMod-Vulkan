package io.github.luasgraphicss.minemod.verification.adapters.layout;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ModuleFiles {
    private static final String PLAN = "src/main/resources/META-INF/minemod/release-plan.properties";
    private static final String BUILD_FILE = "build.gradle.kts";
    private static final String GAME_PLUGIN = "minemod.release-adapter";
    private static final String LOADER_PLUGIN = "minemod.loader-adapter";

    private ModuleFiles() {}

    public static Path planOf(Path repositoryRoot, String module) {
        return repositoryRoot.resolve(module).resolve(PLAN);
    }

    public static Path buildFileOf(Path repositoryRoot, String module) {
        return repositoryRoot.resolve(module).resolve(BUILD_FILE);
    }

    public static String gameBuildFile() {
        return buildFileApplying(GAME_PLUGIN);
    }

    public static String loaderBuildFile() {
        return buildFileApplying(LOADER_PLUGIN);
    }

    public static boolean hasItsOwnBuild(Path repositoryRoot, String module) throws IOException {
        Path buildFile = buildFileOf(repositoryRoot, module);
        if (!Files.isRegularFile(buildFile)) {
            return false;
        }
        String content = Files.readString(buildFile);
        return !content.equals(gameBuildFile()) && !content.equals(loaderBuildFile());
    }

    private static String buildFileApplying(String plugin) {
        return "plugins {\n    id(\"" + plugin + "\")\n}\n";
    }
}
