package io.github.luasgraphicss.minemod.verification.machines.policy;

import static io.github.luasgraphicss.minemod.verification.machines.fixtures.SampleMachines.machine;
import static io.github.luasgraphicss.minemod.verification.machines.fixtures.SampleMachines.registryOf;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.luasgraphicss.minemod.verification.machines.model.MachineRegistry;
import io.github.luasgraphicss.minemod.verification.machines.model.ReferenceMachine.DeviceType;
import io.github.luasgraphicss.minemod.verification.machines.model.ReferenceMachine.Evidence;
import io.github.luasgraphicss.minemod.verification.machines.model.ReferenceMachine.SystemFamily;
import java.util.List;
import org.junit.jupiter.api.Test;

class HardwareCoverageTest {
    private static final String EVERY_EVIDENCE = "native-presentation, window-presentation, shared-image-interop";

    @Test
    void aSoftwareRasterizerCoversNeitherSystem() {
        MachineRegistry registry = registryOf(machine(
                "software-lavapipe",
                SystemFamily.LINUX,
                DeviceType.CPU,
                Evidence.NATIVE_PRESENTATION,
                Evidence.WINDOW_PRESENTATION,
                Evidence.SHARED_IMAGE_INTEROP));

        assertEquals(
                List.of(
                        "no machine with a real GPU on linux recorded " + EVERY_EVIDENCE,
                        "no machine with a real GPU on windows recorded " + EVERY_EVIDENCE),
                HardwareCoverage.findGaps(registry));
    }

    @Test
    void aVirtualGpuIsNotARealOne() {
        MachineRegistry registry = registryOf(
                machine("guest", SystemFamily.LINUX, DeviceType.VIRTUAL_GPU, Evidence.values()),
                machine("bench-windows", SystemFamily.WINDOWS, DeviceType.INTEGRATED_GPU, Evidence.values()));

        assertEquals(
                List.of("no machine with a real GPU on linux recorded " + EVERY_EVIDENCE),
                HardwareCoverage.findGaps(registry));
    }

    @Test
    void namesTheEvidenceARealGpuStillLacks() {
        MachineRegistry registry = registryOf(
                machine("bench-linux", SystemFamily.LINUX, DeviceType.DISCRETE_GPU, Evidence.NATIVE_PRESENTATION),
                machine("bench-windows", SystemFamily.WINDOWS, DeviceType.DISCRETE_GPU, Evidence.values()));

        assertEquals(
                List.of("no machine with a real GPU on linux recorded window-presentation, shared-image-interop"),
                HardwareCoverage.findGaps(registry));
    }

    @Test
    void twoMachinesOfOneSystemCoverItTogether() {
        MachineRegistry registry = registryOf(
                machine(
                        "laptop",
                        SystemFamily.LINUX,
                        DeviceType.INTEGRATED_GPU,
                        Evidence.NATIVE_PRESENTATION,
                        Evidence.WINDOW_PRESENTATION),
                machine("desktop", SystemFamily.LINUX, DeviceType.DISCRETE_GPU, Evidence.SHARED_IMAGE_INTEROP),
                machine("bench-windows", SystemFamily.WINDOWS, DeviceType.DISCRETE_GPU, Evidence.values()));

        assertEquals(List.of(), HardwareCoverage.findGaps(registry));
    }
}
