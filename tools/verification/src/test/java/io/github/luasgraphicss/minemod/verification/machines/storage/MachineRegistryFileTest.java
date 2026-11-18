package io.github.luasgraphicss.minemod.verification.machines.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.luasgraphicss.minemod.verification.machines.fixtures.SampleMachines;
import io.github.luasgraphicss.minemod.verification.machines.model.ReferenceMachine;
import io.github.luasgraphicss.minemod.verification.machines.model.ReferenceMachine.Evidence;
import io.github.luasgraphicss.minemod.verification.machines.model.ReferenceMachine.SystemFamily;
import io.github.luasgraphicss.minemod.verification.machines.policy.MachineRegistryRules;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MachineRegistryFileTest {
    private static final String REGISTRY_PATH = "manifests/reference-machines.json";
    private static final String SOFTWARE_MACHINE = """
            {
              "schemaVersion": 1,
              "machines": [
                {
                  "id": "software-lavapipe",
                  "system": {
                    "family": "linux",
                    "description": "Amazon Linux 2023, x86-64, in a container"
                  },
                  "device": {
                    "name": "llvmpipe (LLVM 15.0.7, 256 bits)",
                    "type": "cpu",
                    "driver": "llvmpipe Mesa 24.2.6 (LLVM 15.0.7)",
                    "vulkanApi": "1.3.289"
                  },
                  "evidence": [
                    "native-presentation",
                    "shared-image-interop"
                  ]
                }
              ]
            }
            """;

    @TempDir
    Path repositoryRoot;

    @Test
    void readsAMachineWithItsSystemDeviceAndEvidence() throws IOException {
        writeRegistry(SOFTWARE_MACHINE);

        ReferenceMachine machine =
                MachineRegistryFile.readFrom(repositoryRoot).machines().get(0);

        assertEquals("software-lavapipe", machine.id());
        assertEquals(SystemFamily.LINUX, machine.system().family());
        assertEquals(SampleMachines.LAVAPIPE, machine.device());
        assertEquals(List.of(Evidence.NATIVE_PRESENTATION, Evidence.SHARED_IMAGE_INTEROP), machine.evidence());
    }

    @Test
    void writesWhatItReadByteForByte() throws IOException {
        writeRegistry(SOFTWARE_MACHINE);

        MachineRegistryFile.writeTo(repositoryRoot, MachineRegistryFile.readFrom(repositoryRoot));

        assertEquals(SOFTWARE_MACHINE, Files.readString(repositoryRoot.resolve(REGISTRY_PATH)));
    }

    @Test
    void theCommittedRegistryFollowsTheRulesInTheFormatTheRecorderWrites(@TempDir Path rewritten) throws IOException {
        Path committedRoot = Path.of(System.getProperty("minemod.repositoryRoot"));

        MachineRegistryFile.writeTo(rewritten, MachineRegistryFile.readFrom(committedRoot));

        assertEquals(List.of(), MachineRegistryRules.findViolations(MachineRegistryFile.readFrom(committedRoot)));
        assertEquals(
                Files.readString(committedRoot.resolve(REGISTRY_PATH)),
                Files.readString(rewritten.resolve(REGISTRY_PATH)));
    }

    @Test
    void rejectsADeviceTypeOutsideTheKnownOnes() throws IOException {
        writeRegistry(SOFTWARE_MACHINE.replace("\"type\": \"cpu\"", "\"type\": \"fpga\""));

        assertThrows(IOException.class, () -> MachineRegistryFile.readFrom(repositoryRoot));
    }

    @Test
    void rejectsAnEvidenceOutsideTheKnownOnes() throws IOException {
        writeRegistry(SOFTWARE_MACHINE.replace("shared-image-interop", "looked-fine"));

        assertThrows(IOException.class, () -> MachineRegistryFile.readFrom(repositoryRoot));
    }

    @Test
    void rejectsAMachineThatOmitsItsDriver() throws IOException {
        writeRegistry(SOFTWARE_MACHINE.replace("\"driver\": \"llvmpipe Mesa 24.2.6 (LLVM 15.0.7)\",", ""));

        assertThrows(IOException.class, () -> MachineRegistryFile.readFrom(repositoryRoot));
    }

    private void writeRegistry(String content) throws IOException {
        Path registry = repositoryRoot.resolve(REGISTRY_PATH);
        Files.createDirectories(registry.getParent());
        Files.writeString(registry, content);
    }
}
