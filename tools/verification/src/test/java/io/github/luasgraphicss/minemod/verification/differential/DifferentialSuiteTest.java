package io.github.luasgraphicss.minemod.verification.differential;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import io.github.luasgraphicss.minemod.compatibility.provider.capability.ContextProfile;
import io.github.luasgraphicss.minemod.verification.differential.ScriptStep.Action;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DifferentialSuiteTest {
    private static final List<ScriptStep> USE_AFTER_DELETE = List.of(
            new ScriptStep(Action.GENERATE, ObjectKind.TEXTURE, 0, 0),
            new ScriptStep(Action.DELETE, ObjectKind.TEXTURE, 0, 0),
            new ScriptStep(Action.BIND, ObjectKind.TEXTURE, 0, 0),
            new ScriptStep(Action.GET_ERROR, ObjectKind.TEXTURE, 0, 0));

    @Test
    void theLayerAnswersLikeTheReferenceOnEveryGeneratedScript() {
        DifferentialResult result = DifferentialSuite.run();

        assertEquals(List.of(), result.differences());
        assertEquals(1_000, result.scripts());
    }

    @Test
    void aLayerThatBehavedLikeAnotherProfileWouldBeNoticed() {
        Optional<String> difference =
                DifferentialSuite.firstDifference(USE_AFTER_DELETE, ContextProfile.CORE, ContextProfile.COMPATIBILITY);

        assertEquals(
                Optional.of("observation 1: the reference has error 0, the layer has error 1282"), difference);
    }

    @Test
    void theSameProfileOnBothSidesAgreesOnThatScript() {
        for (ContextProfile profile : ContextProfile.values()) {
            assertTrue(DifferentialSuite.firstDifference(USE_AFTER_DELETE, profile, profile).isEmpty(), profile.name());
        }
    }

    @Test
    void aSeedAlwaysGeneratesTheSameScript() {
        assertEquals(
                ScriptGenerator.generate(42, ContextProfile.COMPATIBILITY, 60),
                ScriptGenerator.generate(42, ContextProfile.COMPATIBILITY, 60));
        assertEquals(61, ScriptGenerator.generate(42, ContextProfile.CORE, 60).size());
    }
}
