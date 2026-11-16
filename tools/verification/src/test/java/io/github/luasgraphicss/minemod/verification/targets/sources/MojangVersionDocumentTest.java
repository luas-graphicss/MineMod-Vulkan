package io.github.luasgraphicss.minemod.verification.targets.sources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.luasgraphicss.minemod.verification.targets.model.ReleaseTarget;
import io.github.luasgraphicss.minemod.verification.targets.model.ReleaseTarget.LwjglBinding;
import io.github.luasgraphicss.minemod.verification.targets.model.ReleaseTarget.PinnedDocument;
import io.github.luasgraphicss.minemod.verification.targets.policy.ReleaseCatalog.RequiredRelease;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

class MojangVersionDocumentTest {
    private static final PinnedDocument METADATA = new PinnedDocument(
            "https://piston-meta.mojang.com/v1/packages/0/version.json", "0000000000000000000000000000000000000000");
    private static final String DOWNLOADS = """
            "downloads": {"client": {"sha1": "0f275bc1547d01fa5f56ba34bdc87d981ee12daf", "size": 10180113,
              "url": "https://launcher.mojang.com/v1/objects/0f275bc1547d01fa5f56ba34bdc87d981ee12daf/client.jar"}}
            """;
    private static final String LEGACY_VERSION = """
            {
              %s,
              "javaVersion": {"component": "jre-legacy", "majorVersion": 8},
              "mainClass": "net.minecraft.launchwrapper.Launch",
              "minecraftArguments": "--username ${auth_player_name} --version ${version_name}",
              "libraries": [
                {"name": "org.lwjgl.lwjgl:lwjgl:2.9.4-nightly-20150209",
                 "rules": [{"action": "allow"}, {"action": "disallow", "os": {"name": "osx"}}]},
                {"name": "org.lwjgl.lwjgl:lwjgl:2.9.2-nightly-20140822",
                 "rules": [{"action": "allow", "os": {"name": "osx"}}]},
                {"name": "org.lwjgl.lwjgl:lwjgl_util:2.9.4-nightly-20150209"}
              ]
            }
            """.formatted(DOWNLOADS);
    private static final String MODERN_VERSION = """
            {
              %s,
              "javaVersion": {"component": "java-runtime-delta", "majorVersion": 21},
              "mainClass": "net.minecraft.client.main.Main",
              "arguments": {
                "game": ["--username", "${auth_player_name}", {"rules": [], "value": "--demo"}],
                "jvm": [{"rules": [], "value": "-XstartOnFirstThread"}, "-cp", "${classpath}"]
              },
              "libraries": [
                {"name": "org.lwjgl:lwjgl:3.3.3"},
                {"name": "org.lwjgl:lwjgl:3.3.3:natives-linux", "rules": [{"action": "allow", "os": {"name": "linux"}}]}
              ]
            }
            """.formatted(DOWNLOADS);
    private static final String AMBIGUOUS_VERSION = MODERN_VERSION.replace(
            "{\"name\": \"org.lwjgl:lwjgl:3.3.3\"},",
            "{\"name\": \"org.lwjgl:lwjgl:3.3.3\"}, {\"name\": \"org.lwjgl:lwjgl:3.3.1\"},");

    @Test
    void readsTheLegacyLaunchProfileAndTheBindingUsedOnWindowsAndLinux() throws IOException {
        ReleaseTarget release = parse(LEGACY_VERSION, new RequiredRelease("1.12.2", "1.12", 8));

        assertEquals(new LwjglBinding(2, "2.9.4-nightly-20150209"), release.lwjgl());
        assertEquals(
                List.of("--username", "${auth_player_name}", "--version", "${version_name}"),
                release.launch().gameArguments());
        assertEquals(8, release.runtime().majorVersion());
        assertNull(release.clientMappings());
    }

    @Test
    void keepsOnlyUnconditionalArgumentsOfAModernLaunchProfile() throws IOException {
        ReleaseTarget release = parse(MODERN_VERSION, new RequiredRelease("1.21.1", "1.21", 21));

        assertEquals(new LwjglBinding(3, "3.3.3"), release.lwjgl());
        assertEquals(List.of("--username", "${auth_player_name}"), release.launch().gameArguments());
        assertEquals(List.of("-cp", "${classpath}"), release.launch().jvmArguments());
    }

    @Test
    void rejectsAReleaseWithTwoBindingsForTheSupportedSystems() {
        assertThrows(
                MetadataFormatException.class,
                () -> parse(AMBIGUOUS_VERSION, new RequiredRelease("1.21.1", "1.21", 21)));
    }

    private static ReleaseTarget parse(String document, RequiredRelease required) throws IOException {
        return MojangVersionDocument.toReleaseTarget(
                new ObjectMapper().readTree(document), required, "2024-08-08T12:24:45+00:00", METADATA, List.of());
    }
}
