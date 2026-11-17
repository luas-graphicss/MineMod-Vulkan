package io.github.luasgraphicss.minemod.verification.adapters.names;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.luasgraphicss.minemod.verification.hooks.fixtures.CommittedManifests;
import io.github.luasgraphicss.minemod.verification.targets.model.LoaderTarget;
import io.github.luasgraphicss.minemod.verification.targets.model.ReleaseTarget;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NamespacePolicyTest {
    @Test
    void everyLoaderOfTheManifestRunsOneOfTheFourNamespaces() throws IOException {
        Map<RuntimeNamespace, Integer> loaders = new EnumMap<>(RuntimeNamespace.class);
        for (ReleaseTarget release : CommittedManifests.targets().releases()) {
            for (LoaderTarget loader : release.loaders()) {
                loaders.merge(NamespacePolicy.of(loader), 1, Integer::sum);
            }
        }

        assertEquals(
                Map.of(
                        RuntimeNamespace.OFFICIAL, 3,
                        RuntimeNamespace.MOJANG, 10,
                        RuntimeNamespace.INTERMEDIARY, 33,
                        RuntimeNamespace.SRG, 27),
                loaders);
    }

    @Test
    void forgeLeavesSrgNamesWhenItStartsRunningTheNamesOfMojang() throws IOException {
        assertEquals(RuntimeNamespace.OFFICIAL, namespaceOf("1.12.2", LoaderTarget.Kind.FORGE));
        assertEquals(RuntimeNamespace.SRG, namespaceOf("1.20.4", LoaderTarget.Kind.FORGE));
        assertEquals(RuntimeNamespace.MOJANG, namespaceOf("1.20.6", LoaderTarget.Kind.FORGE));
    }

    @Test
    void neoForgeRunsSrgNamesOnlyWhileItIsAForkOfForge() throws IOException {
        assertEquals(RuntimeNamespace.SRG, namespaceOf("1.20.1", LoaderTarget.Kind.NEOFORGE));
        assertEquals(RuntimeNamespace.MOJANG, namespaceOf("1.20.2", LoaderTarget.Kind.NEOFORGE));
    }

    @Test
    void theClientBootstrapAndFabricNeverChange() throws IOException {
        assertEquals(RuntimeNamespace.OFFICIAL, namespaceOf("1.13", LoaderTarget.Kind.CLIENT_BOOTSTRAP));
        assertEquals(RuntimeNamespace.INTERMEDIARY, namespaceOf("1.14", LoaderTarget.Kind.FABRIC));
        assertEquals(RuntimeNamespace.INTERMEDIARY, namespaceOf("1.21.1", LoaderTarget.Kind.FABRIC));
    }

    private static RuntimeNamespace namespaceOf(String release, LoaderTarget.Kind kind) throws IOException {
        return CommittedManifests.targets().releases().stream()
                .filter(target -> target.id().equals(release))
                .flatMap(target -> target.loaders().stream())
                .filter(loader -> loader.loader() == kind)
                .map(NamespacePolicy::of)
                .findFirst()
                .orElseThrow();
    }
}
