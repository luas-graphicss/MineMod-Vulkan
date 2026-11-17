package io.github.luasgraphicss.minemod.verification.machines.record;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.luasgraphicss.minemod.verification.machines.fixtures.SampleMachines;
import io.github.luasgraphicss.minemod.verification.machines.model.ReferenceMachine;
import io.github.luasgraphicss.minemod.verification.machines.model.ReferenceMachine.Evidence;
import io.github.luasgraphicss.minemod.verification.machines.model.ReferenceMachine.HostSystem;
import io.github.luasgraphicss.minemod.verification.machines.model.ReferenceMachine.SystemFamily;
import io.github.luasgraphicss.minemod.verification.machines.probe.HarnessReport;
import io.github.luasgraphicss.minemod.verification.machines.probe.SuiteResults.SuiteOutcome;
import io.github.luasgraphicss.minemod.verification.machines.record.MachineRecorder.Recording;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class MachineRecorderTest {
    private static final String GLFW_SUITE =
            "io.github.luasgraphicss.minemod.adapters.presentation.glfw.NativeGlfwOnWindowTest";
    private static final String DISPLAY_SUITE =
            "io.github.luasgraphicss.minemod.adapters.presentation.display.NativeDisplayOnWindowTest";
    private static final String SHARED_SCENE_SUITE =
            "io.github.luasgraphicss.minemod.compatibility.runtime.SharedSceneOnZinkTest";
    private static final SuiteOutcome EVERY_TEST_RAN = new SuiteOutcome(1, 0, 0, 0);
    private static final HostSystem SYSTEM = new HostSystem(SystemFamily.LINUX, "Amazon Linux 2023, x86-64");
    private static final HarnessReport CLEAN_HARNESS = new HarnessReport(SampleMachines.LAVAPIPE, 3, OptionalInt.of(0));

    @Test
    void recordsWhatTheHarnessAndTheCompleteSuitesProve() {
        Map<String, SuiteOutcome> suites = Map.of(
                GLFW_SUITE, EVERY_TEST_RAN, DISPLAY_SUITE, EVERY_TEST_RAN, SHARED_SCENE_SUITE, new SuiteOutcome(5, 0, 0, 0));

        Recording recording = MachineRecorder.record("software-lavapipe", SYSTEM, CLEAN_HARNESS, suites);

        assertEquals(
                new ReferenceMachine(
                        "software-lavapipe",
                        SYSTEM,
                        SampleMachines.LAVAPIPE,
                        List.of(
                                Evidence.NATIVE_PRESENTATION,
                                Evidence.WINDOW_PRESENTATION,
                                Evidence.SHARED_IMAGE_INTEROP)),
                recording.machine());
        assertEquals(List.of(), recording.unproven());
    }

    @Test
    void leavesOutTheEvidenceOfASuiteThatSkippedItsTests() {
        Map<String, SuiteOutcome> suites = Map.of(
                GLFW_SUITE, EVERY_TEST_RAN, DISPLAY_SUITE, EVERY_TEST_RAN, SHARED_SCENE_SUITE, new SuiteOutcome(5, 5, 0, 0));

        Recording recording = MachineRecorder.record("bench", SYSTEM, CLEAN_HARNESS, suites);

        assertEquals(
                List.of(Evidence.NATIVE_PRESENTATION, Evidence.WINDOW_PRESENTATION),
                recording.machine().evidence());
        assertEquals(
                List.of("shared-image-interop: SharedSceneOnZinkTest ran 5 test(s) with 5 skipped and 0 failed"),
                recording.unproven());
    }

    @Test
    void windowPresentationNeedsBothWindowSuites() {
        Recording recording = MachineRecorder.record("bench", SYSTEM, CLEAN_HARNESS, Map.of(GLFW_SUITE, EVERY_TEST_RAN));

        assertEquals(List.of(Evidence.NATIVE_PRESENTATION), recording.machine().evidence());
        assertEquals(
                List.of(
                        "window-presentation: NativeDisplayOnWindowTest has no report",
                        "shared-image-interop: SharedSceneOnZinkTest has no report"),
                recording.unproven());
    }

    @Test
    void aFailedTestProvesNothing() {
        Map<String, SuiteOutcome> suites = Map.of(
                GLFW_SUITE, EVERY_TEST_RAN, DISPLAY_SUITE, new SuiteOutcome(1, 0, 1, 0), SHARED_SCENE_SUITE, new SuiteOutcome(5, 0, 0, 2));

        Recording recording = MachineRecorder.record("bench", SYSTEM, CLEAN_HARNESS, suites);

        assertEquals(
                List.of(
                        "window-presentation: NativeDisplayOnWindowTest ran 1 test(s) with 0 skipped and 1 failed",
                        "shared-image-interop: SharedSceneOnZinkTest ran 5 test(s) with 0 skipped and 2 failed"),
                recording.unproven());
    }

    @Test
    void validationMessagesKeepNativePresentationOut() {
        HarnessReport noisy = new HarnessReport(SampleMachines.LAVAPIPE, 3, OptionalInt.of(2));

        Recording recording = MachineRecorder.record("bench", SYSTEM, noisy, Map.of());

        assertEquals(List.of(), recording.machine().evidence());
        assertEquals(
                "native-presentation: the harness presented 3 frame(s) with 2 validation message(s)",
                recording.unproven().get(0));
    }
}
