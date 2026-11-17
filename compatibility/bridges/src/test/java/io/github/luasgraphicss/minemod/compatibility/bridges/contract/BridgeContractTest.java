package io.github.luasgraphicss.minemod.compatibility.bridges.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.compatibility.bridges.contract.ContractVerdict.Status;
import io.github.luasgraphicss.minemod.compatibility.bridges.support.ListedMods;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class BridgeContractTest {
    private static final ContractSymbol PACK_IN_USE =
            new ContractSymbol("net.example.shaders.api.ShaderApi", "isShaderPackInUse", "()Z");
    private static final ContractSymbol SHADOW_PASS =
            new ContractSymbol("net.example.shaders.api.ShaderApi", "isRenderingShadowPass", "()Z");

    private final BridgeContract contract =
            new BridgeContract("shader-loader", Arrays.asList("1.7.3", "1.7.4"), Arrays.asList(PACK_IN_USE, SHADOW_PASS));

    @Test
    void isSatisfiedByACoveredVersionThatKeepsEverySymbol() {
        ContractVerdict verdict =
                contract.verdictFor(new ListedMods().install("shader-loader", "1.7.4", PACK_IN_USE, SHADOW_PASS));

        assertTrue(verdict.activates());
        assertFalse(verdict.leavesInstalledModUnbridged());
    }

    @Test
    void staysIdleWhenTheModIsNotInstalled() {
        ContractVerdict verdict = contract.verdictFor(new ListedMods().install("recipe-viewer", "17.0.0"));

        assertEquals(Status.MOD_ABSENT, verdict.status());
        assertFalse(verdict.leavesInstalledModUnbridged());
    }

    @Test
    void refusesAVersionItWasNotVerifiedOnEvenWhenTheSymbolsAreThere() {
        ContractVerdict verdict =
                contract.verdictFor(new ListedMods().install("shader-loader", "1.8.0", PACK_IN_USE, SHADOW_PASS));

        assertEquals(Status.VERSION_NOT_COVERED, verdict.status());
        assertTrue(verdict.leavesInstalledModUnbridged());
        assertEquals("shader-loader 1.8.0 is not a version the contract was verified on", verdict.explanation());
    }

    @Test
    void refusesACoveredVersionThatLostASymbol() {
        ContractVerdict verdict = contract.verdictFor(new ListedMods().install("shader-loader", "1.7.3", PACK_IN_USE));

        assertEquals(Status.SYMBOLS_MISSING, verdict.status());
        assertEquals(
                "shader-loader 1.7.3 does not declare [net.example.shaders.api.ShaderApi#isRenderingShadowPass()Z]",
                verdict.explanation());
    }

    @Test
    void aContractWithoutVersionsOrSymbolsIsNotAContract() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new BridgeContract(
                        "shader-loader", Collections.<String>emptyList(), Collections.singletonList(PACK_IN_USE)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new BridgeContract(
                        "shader-loader", Collections.singletonList("1.7.3"), Collections.<ContractSymbol>emptyList()));
        assertThrows(IllegalArgumentException.class, () -> new ContractSymbol("net.example.Api", "field", "Z"));
    }
}
