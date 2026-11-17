package io.github.luasgraphicss.minemod.verification.machines.probe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.verification.machines.fixtures.SampleMachines;
import io.github.luasgraphicss.minemod.verification.machines.model.ReferenceMachine.DeviceType;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class HarnessReportTest {
    private static final String DEVICE_LINE = "device: llvmpipe (LLVM 15.0.7, 256 bits) (CPU)";
    private static final String CLEAN_VALIDATION = "validation: 0 message(s)";
    private static final String ON_LAVAPIPE = """
            device: llvmpipe (LLVM 15.0.7, 256 bits) (CPU)
            driver: llvmpipe Mesa 24.2.6 (LLVM 15.0.7)
            vulkan: 1.3.289
            frame: 256x256, color VkFormat 37, depth VkFormat 126
            presentation: 3 frame(s) on a headless surface, VkFormat 37
            validation: 0 message(s)
            """;

    @Test
    void readsTheDeviceTheRendererSelectedAndItsCleanPresentation() throws IOException {
        HarnessReport report = HarnessReport.parse(ON_LAVAPIPE);

        assertEquals(SampleMachines.LAVAPIPE, report.device());
        assertEquals(3, report.presentedFrames());
        assertTrue(report.presentedWithoutValidationMessages());
    }

    @Test
    void readsTheTypeOfAHardwareDevice() throws IOException {
        HarnessReport report =
                HarnessReport.parse(ON_LAVAPIPE.replace(DEVICE_LINE, "device: Radeon RX 6600 (discrete GPU)"));

        assertEquals("Radeon RX 6600", report.device().name());
        assertEquals(DeviceType.DISCRETE_GPU, report.device().type());
    }

    @Test
    void validationMessagesSpoilThePresentation() throws IOException {
        HarnessReport report = HarnessReport.parse(
                ON_LAVAPIPE.replace(CLEAN_VALIDATION, "validation: 2 message(s)\n  first\n  second"));

        assertFalse(report.presentedWithoutValidationMessages());
    }

    @Test
    void aRunWithoutValidationProvesNoCleanPresentation() throws IOException {
        HarnessReport report = HarnessReport.parse(ON_LAVAPIPE.replace(CLEAN_VALIDATION, "validation: not requested"));

        assertFalse(report.presentedWithoutValidationMessages());
    }

    @Test
    void rejectsOutputWithoutTheDeviceLine() {
        IOException refused =
                assertThrows(IOException.class, () -> HarnessReport.parse(ON_LAVAPIPE.replace(DEVICE_LINE, "")));

        assertEquals("the harness did not report its device line", refused.getMessage());
    }

    @Test
    void rejectsADeviceTypeItDoesNotKnow() {
        String output = ON_LAVAPIPE.replace(DEVICE_LINE, "device: llvmpipe (accelerator)");

        IOException refused = assertThrows(IOException.class, () -> HarnessReport.parse(output));

        assertEquals("the harness reported a device type this tool does not know: accelerator", refused.getMessage());
    }
}
