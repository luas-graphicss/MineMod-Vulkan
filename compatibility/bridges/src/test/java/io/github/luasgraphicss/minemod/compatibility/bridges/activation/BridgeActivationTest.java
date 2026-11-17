package io.github.luasgraphicss.minemod.compatibility.bridges.activation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.compatibility.bridges.contract.BridgeContract;
import io.github.luasgraphicss.minemod.compatibility.bridges.contract.ContractSymbol;
import io.github.luasgraphicss.minemod.compatibility.bridges.contract.ContractVerdict;
import io.github.luasgraphicss.minemod.compatibility.bridges.contract.ContractVerdict.Status;
import io.github.luasgraphicss.minemod.compatibility.bridges.registry.BridgeRegistry;
import io.github.luasgraphicss.minemod.compatibility.bridges.support.ListedMods;
import io.github.luasgraphicss.minemod.compatibility.bridges.support.RecordingBridge;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BridgeActivationTest {
    private static final String CHUNK_MESHES = "chunk meshes of the chunk renderer";
    private static final String SHADER_PASSES = "passes of the shader pack";
    private static final ContractSymbol MESH_OUTPUT =
            new ContractSymbol("net.example.chunks.MeshOutput", "vertices", "()Ljava/nio/ByteBuffer;");
    private static final ContractSymbol PACK_IN_USE =
            new ContractSymbol("net.example.shaders.api.ShaderApi", "isShaderPackInUse", "()Z");

    private final List<String> journal = new ArrayList<>();
    private final BridgeRegistry registry = new BridgeRegistry();
    private final BridgeActivation activation = new BridgeActivation(registry);
    private final List<BridgeCandidate> candidates = Arrays.asList(
            new BridgeCandidate(
                    new RecordingBridge("chunk-meshes", journal, CHUNK_MESHES),
                    new BridgeContract(
                            "chunk-renderer", Collections.singletonList("2.1.0"), Collections.singletonList(MESH_OUTPUT))),
            new BridgeCandidate(
                    new RecordingBridge("shader-passes", journal, SHADER_PASSES),
                    new BridgeContract(
                            "shader-loader", Collections.singletonList("1.7.3"), Collections.singletonList(PACK_IN_USE))));

    @Test
    void registersOnlyTheBridgesWhoseContractHolds() {
        Map<String, ContractVerdict> verdicts = activation.activate(
                candidates,
                new ListedMods()
                        .install("chunk-renderer", "2.1.0", MESH_OUTPUT)
                        .install("shader-loader", "1.8.0", PACK_IN_USE));

        assertTrue(registry.bridgeFor(CHUNK_MESHES).isPresent());
        assertFalse(registry.bridgeFor(SHADER_PASSES).isPresent());
        assertEquals(Status.SATISFIED, verdicts.get("chunk-meshes").status());
        assertEquals(Status.VERSION_NOT_COVERED, verdicts.get("shader-passes").status());
    }

    @Test
    void aClassWithTheRightNameButAnotherSignatureActivatesNothing() {
        ContractSymbol renamedOutput = new ContractSymbol("net.example.chunks.MeshOutput", "vertices", "()[B");

        Map<String, ContractVerdict> verdicts =
                activation.activate(candidates, new ListedMods().install("chunk-renderer", "2.1.0", renamedOutput));

        assertTrue(registry.bridges().isEmpty());
        assertEquals(Status.SYMBOLS_MISSING, verdicts.get("chunk-meshes").status());
        assertEquals(Status.MOD_ABSENT, verdicts.get("shader-passes").status());
    }
}
