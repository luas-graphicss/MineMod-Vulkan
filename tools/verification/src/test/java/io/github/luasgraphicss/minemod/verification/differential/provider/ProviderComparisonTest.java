package io.github.luasgraphicss.minemod.verification.differential.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import io.github.luasgraphicss.minemod.compatibility.provider.capability.ContextProfile;
import io.github.luasgraphicss.minemod.compatibility.runtime.fixtures.ProviderTestEnvironment;
import io.github.luasgraphicss.minemod.verification.differential.ScriptStep;
import io.github.luasgraphicss.minemod.verification.differential.ScriptStep.Action;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@EnabledIfSystemProperty(named = ProviderTestEnvironment.SYSTEM_PROVIDER, matches = ".+")
class ProviderComparisonTest {
    private static final String NATIVES = "minemod.providerNatives";
    private static final List<ScriptStep> USE_AFTER_DELETE = List.of(
            new ScriptStep(Action.GENERATE, ObjectKind.TEXTURE, 0, 0),
            new ScriptStep(Action.DELETE, ObjectKind.TEXTURE, 0, 0),
            new ScriptStep(Action.BIND, ObjectKind.TEXTURE, 0, 0),
            new ScriptStep(Action.GET_ERROR, ObjectKind.TEXTURE, 0, 0));
    private static final List<ScriptStep> TRIANGLE_BETWEEN_TWO_READS = List.of(
            new ScriptStep(Action.READ, ObjectKind.TEXTURE, 0, 0),
            new ScriptStep(Action.IMMEDIATE_TRIANGLE, ObjectKind.TEXTURE, 0, 0),
            new ScriptStep(Action.READ, ObjectKind.TEXTURE, 0, 0),
            new ScriptStep(Action.GET_ERROR, ObjectKind.TEXTURE, 0, 0));
    private static final List<ScriptStep> FIRST_BINDS_OF_UNUSED_NAMES = List.of(
            new ScriptStep(Action.BIND_UNGENERATED, ObjectKind.RENDERBUFFER, 0, 0),
            new ScriptStep(Action.BIND_UNGENERATED, ObjectKind.FRAMEBUFFER, 1, 0),
            new ScriptStep(Action.GET_ERROR, ObjectKind.TEXTURE, 0, 0));
    private static final List<ScriptStep> USES_OF_A_DELETED_PROGRAM = List.of(
            new ScriptStep(Action.GENERATE, ObjectKind.SHADER_OR_PROGRAM, 0, 0),
            new ScriptStep(Action.DELETE, ObjectKind.SHADER_OR_PROGRAM, 0, 0),
            new ScriptStep(Action.BIND, ObjectKind.SHADER_OR_PROGRAM, 0, 0),
            new ScriptStep(Action.GET_ERROR, ObjectKind.TEXTURE, 0, 0),
            new ScriptStep(Action.DELETE, ObjectKind.SHADER_OR_PROGRAM, 0, 0),
            new ScriptStep(Action.GET_ERROR, ObjectKind.TEXTURE, 0, 0));
    private static final List<ScriptStep> DRAW_AFTER_DELETING_THE_BOUND_VERTEX_ARRAY = List.of(
            new ScriptStep(Action.GENERATE, ObjectKind.VERTEX_ARRAY, 0, 0),
            new ScriptStep(Action.BIND, ObjectKind.VERTEX_ARRAY, 0, 0),
            new ScriptStep(Action.DELETE, ObjectKind.VERTEX_ARRAY, 0, 0),
            new ScriptStep(Action.DRAW, ObjectKind.TEXTURE, 0, 0),
            new ScriptStep(Action.GET_ERROR, ObjectKind.TEXTURE, 0, 0));

    private SystemProvider provider;

    @BeforeEach
    void stageTheInstalledMesa() throws IOException {
        provider = SystemProvider.stage(
                Paths.get(System.getProperty(ProviderTestEnvironment.SYSTEM_PROVIDER)),
                Paths.get(System.getProperty(NATIVES)));
    }

    @AfterEach
    void discardTheStagedCopy() throws IOException {
        provider.close();
    }

    @Test
    void theLayerAnswersLikeTheProviderWhenADeletedTextureIsBound() throws IOException {
        for (ContextProfile profile : ContextProfile.values()) {
            try (ProviderComparison comparison = provider.compareOn(profile)) {
                assertEquals(Optional.empty(), comparison.firstDifference(USE_AFTER_DELETE), profile.name());
            }
        }
    }

    @Test
    void aTriangleDrawnThroughTheLayerIsReadBackLikeOneDrawnOnTheProvider() throws IOException {
        List<String> clearedThenCovered = List.of("read 00000000", "read ffffffff", "error 0");

        assertBothSidesObserve(ContextProfile.COMPATIBILITY, TRIANGLE_BETWEEN_TWO_READS, clearedThenCovered);
    }

    @Test
    void aCompatibilityContextCreatesARenderbufferAndAFramebufferOnTheirFirstBind() throws IOException {
        assertBothSidesObserve(ContextProfile.COMPATIBILITY, FIRST_BINDS_OF_UNUSED_NAMES, List.of("error 0"));
    }

    @Test
    void aDeletedProgramIsAnInvalidValueWhenItIsUsedOrDeletedAgain() throws IOException {
        List<String> invalidValues = List.of("generated SHADER_OR_PROGRAM #0", "error 1281", "error 1281");

        assertBothSidesObserve(ContextProfile.CORE, USES_OF_A_DELETED_PROGRAM, invalidValues);
    }

    @Test
    void aCoreContextRefusesToDrawOnceItsBoundVertexArrayIsDeleted() throws IOException {
        List<String> refusedDraw = List.of("generated VERTEX_ARRAY #0", "error 1282");

        assertBothSidesObserve(ContextProfile.CORE, DRAW_AFTER_DELETING_THE_BOUND_VERTEX_ARRAY, refusedDraw);
    }

    private void assertBothSidesObserve(ContextProfile profile, List<ScriptStep> script, List<String> expected)
            throws IOException {
        try (ProviderComparison comparison = provider.compareOn(profile)) {
            assertEquals(expected, comparison.playThroughTheLayer(script), "through the layer");
            assertEquals(expected, comparison.playOnTheProvider(script), "on the provider");
        }
    }
}
