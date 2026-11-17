package io.github.luasgraphicss.minemod.verification.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.github.luasgraphicss.minemod.verification.machines.model.MachineRegistry;
import io.github.luasgraphicss.minemod.verification.machines.model.ReferenceMachine;
import io.github.luasgraphicss.minemod.verification.machines.model.ReferenceMachine.DeviceType;
import io.github.luasgraphicss.minemod.verification.machines.model.ReferenceMachine.Evidence;
import io.github.luasgraphicss.minemod.verification.machines.storage.MachineRegistryFile;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MachinesCommandTest {
    private static final String SUITES = "io.github.luasgraphicss.minemod.";
    private static final String HARNESS_ON_A_DISCRETE_GPU = """
            #!/bin/sh
            cat <<'REPORT'
            device: Radeon RX 6600 (discrete GPU)
            driver: radv Mesa 24.2.6
            vulkan: 1.3.289
            frame: 256x256, color VkFormat 37, depth VkFormat 126
            presentation: 3 frame(s) on a headless surface, VkFormat 37
            validation: 0 message(s)
            REPORT
            """;

    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errors = new ByteArrayOutputStream();

    @TempDir
    Path repositoryRoot;

    @BeforeEach
    void startFromAnEmptyRegistry() throws IOException {
        assumeTrue(System.getProperty("os.name").equals("Linux"), "the stand-in for the harness is a shell script");
        MachineRegistryFile.writeTo(repositoryRoot, new MachineRegistry(MachineRegistry.SCHEMA_VERSION, List.of()));
    }

    @Test
    void recordsTheDeviceTheHarnessRanOnAndSaysWhatStayedUnproven() throws IOException {
        Path harness = writeHarness(HARNESS_ON_A_DISCRETE_GPU);

        int exitCode = run("machines", "record", "--id", "bench-linux", "--harness", harness.toString());

        assertEquals(VerificationCli.EXIT_OK, exitCode, text(errors));
        ReferenceMachine machine = recordedMachines().get(0);
        assertEquals("Radeon RX 6600", machine.device().name());
        assertEquals(DeviceType.DISCRETE_GPU, machine.device().type());
        assertEquals(List.of(Evidence.NATIVE_PRESENTATION), machine.evidence());
        assertTrue(
                text(output).contains("not recorded, shared-image-interop: SharedSceneOnZinkTest has no report"),
                text(output));
        assertTrue(
                text(output)
                        .endsWith("machine bench-linux: Radeon RX 6600 (discrete-gpu) on linux, native-presentation"
                                + System.lineSeparator()),
                text(output));
    }

    @Test
    void recordsTheEvidenceOfTheSuitesThatRanEveryTest() throws IOException {
        Path harness = writeHarness(HARNESS_ON_A_DISCRETE_GPU);
        writeSuite("adapters.presentation.glfw.NativeGlfwOnWindowTest", 0);
        writeSuite("adapters.presentation.display.NativeDisplayOnWindowTest", 0);
        writeSuite("compatibility.runtime.SharedSceneOnZinkTest", 1);

        int exitCode = run(
                "machines", "record", "--id", "bench-linux", "--harness", harness.toString(), "--results", "results");

        assertEquals(VerificationCli.EXIT_OK, exitCode, text(errors));
        assertEquals(
                List.of(Evidence.NATIVE_PRESENTATION, Evidence.WINDOW_PRESENTATION),
                recordedMachines().get(0).evidence());
    }

    @Test
    void replacesTheEntryOfAMachineRecordedBefore() throws IOException {
        Path harness = writeHarness(HARNESS_ON_A_DISCRETE_GPU);
        run("machines", "record", "--id", "bench-linux", "--harness", harness.toString());

        run("machines", "record", "--id", "bench-linux", "--harness", harness.toString(), "--system", "Fedora 41");

        assertEquals(1, recordedMachines().size());
        assertEquals("Fedora 41", recordedMachines().get(0).system().description());
    }

    @Test
    void refusesAnIdThatBreaksTheRulesAndLeavesTheRegistryAlone() throws IOException {
        Path harness = writeHarness(HARNESS_ON_A_DISCRETE_GPU);

        int exitCode = run("machines", "record", "--id", "Bench Linux", "--harness", harness.toString());

        assertEquals(VerificationCli.EXIT_VIOLATIONS, exitCode);
        assertEquals(List.of(), recordedMachines());
    }

    @Test
    void reportsAHarnessThatFails() throws IOException {
        Path harness = writeHarness("#!/bin/sh\necho 'no Vulkan 1.3 device'\nexit 3\n");

        int exitCode = run("machines", "record", "--id", "bench-linux", "--harness", harness.toString());

        assertEquals(VerificationCli.EXIT_FAILURE, exitCode);
        assertEquals(
                "verification failed: the harness exited with 3: no Vulkan 1.3 device" + System.lineSeparator(),
                text(errors));
        assertEquals(List.of(), recordedMachines());
    }

    @Test
    void requiresTheHarness() {
        assertEquals(VerificationCli.EXIT_USAGE, run("machines", "record", "--id", "bench-linux"));
        assertTrue(text(errors).startsWith("missing required option: --harness"), text(errors));
    }

    @Test
    void theHardwareProfileNamesTheSystemsWithoutEvidenceOnARealGpu() throws IOException {
        Path harness = writeHarness(HARNESS_ON_A_DISCRETE_GPU);
        run("machines", "record", "--id", "bench-linux", "--harness", harness.toString());

        int exitCode = run("check", "--profile", "hardware");

        assertEquals(VerificationCli.EXIT_VIOLATIONS, exitCode);
        assertTrue(text(output).contains("check reference-machines: ok"), text(output));
        assertTrue(
                text(output)
                        .contains("no machine with a real GPU on linux recorded window-presentation,"
                                + " shared-image-interop"),
                text(output));
        assertTrue(
                text(output)
                        .contains("no machine with a real GPU on windows recorded native-presentation,"
                                + " window-presentation, shared-image-interop"),
                text(output));
    }

    private Path writeHarness(String script) throws IOException {
        Path harness = repositoryRoot.resolve("harness.sh");
        Files.writeString(harness, script);
        assertTrue(harness.toFile().setExecutable(true));
        return harness;
    }

    private void writeSuite(String name, int skipped) throws IOException {
        Path report = repositoryRoot.resolve("results/TEST-" + SUITES + name + ".xml");
        Files.createDirectories(report.getParent());
        Files.writeString(
                report,
                "<testsuite name=\"" + SUITES + name + "\" tests=\"1\" skipped=\"" + skipped
                        + "\" failures=\"0\" errors=\"0\"/>");
    }

    private List<ReferenceMachine> recordedMachines() throws IOException {
        return MachineRegistryFile.readFrom(repositoryRoot).machines();
    }

    private int run(String... arguments) {
        String[] withRoot = new String[arguments.length + 2];
        System.arraycopy(arguments, 0, withRoot, 0, arguments.length);
        withRoot[arguments.length] = "--root";
        withRoot[arguments.length + 1] = repositoryRoot.toString();
        return VerificationCli.run(
                withRoot,
                new PrintStream(output, true, StandardCharsets.UTF_8),
                new PrintStream(errors, true, StandardCharsets.UTF_8));
    }

    private static String text(ByteArrayOutputStream stream) {
        return stream.toString(StandardCharsets.UTF_8);
    }
}
