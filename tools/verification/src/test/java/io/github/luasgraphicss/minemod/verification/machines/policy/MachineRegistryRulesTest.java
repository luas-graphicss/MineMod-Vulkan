package io.github.luasgraphicss.minemod.verification.machines.policy;

import static io.github.luasgraphicss.minemod.verification.machines.fixtures.SampleMachines.machine;
import static io.github.luasgraphicss.minemod.verification.machines.fixtures.SampleMachines.registryOf;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.luasgraphicss.minemod.verification.machines.model.MachineRegistry;
import io.github.luasgraphicss.minemod.verification.machines.model.ReferenceMachine;
import io.github.luasgraphicss.minemod.verification.machines.model.ReferenceMachine.DeviceType;
import io.github.luasgraphicss.minemod.verification.machines.model.ReferenceMachine.Evidence;
import io.github.luasgraphicss.minemod.verification.machines.model.ReferenceMachine.HostSystem;
import io.github.luasgraphicss.minemod.verification.machines.model.ReferenceMachine.SystemFamily;
import io.github.luasgraphicss.minemod.verification.machines.model.ReferenceMachine.VulkanDevice;
import java.util.List;
import org.junit.jupiter.api.Test;

class MachineRegistryRulesTest {
    @Test
    void acceptsMachinesWithDistinctIdsAndCompleteDevices() {
        MachineRegistry registry = registryOf(
                machine("software-lavapipe", SystemFamily.LINUX, DeviceType.CPU, Evidence.NATIVE_PRESENTATION),
                machine("bench-linux", SystemFamily.LINUX, DeviceType.DISCRETE_GPU));

        assertEquals(List.of(), MachineRegistryRules.findViolations(registry));
    }

    @Test
    void rejectsAnotherSchemaVersion() {
        MachineRegistry registry = new MachineRegistry(2, List.of());

        assertEquals(List.of("schemaVersion is 2, expected 1"), MachineRegistryRules.findViolations(registry));
    }

    @Test
    void rejectsAnIdThatIsNotHyphenatedLowercase() {
        MachineRegistry registry = registryOf(machine("Bench GPU", SystemFamily.LINUX, DeviceType.DISCRETE_GPU));

        assertEquals(
                List.of("machine id \"Bench GPU\" is not lowercase words joined by hyphens"),
                MachineRegistryRules.findViolations(registry));
    }

    @Test
    void rejectsTwoMachinesWithOneId() {
        MachineRegistry registry = registryOf(
                machine("bench", SystemFamily.LINUX, DeviceType.DISCRETE_GPU),
                machine("bench", SystemFamily.WINDOWS, DeviceType.DISCRETE_GPU));

        assertEquals(List.of("machine bench is listed twice"), MachineRegistryRules.findViolations(registry));
    }

    @Test
    void rejectsADeviceWithoutItsDriverOrWithAMalformedApiVersion() {
        ReferenceMachine incomplete = new ReferenceMachine(
                "bench",
                new HostSystem(SystemFamily.LINUX, " "),
                new VulkanDevice("", DeviceType.DISCRETE_GPU, "", "1.3"),
                List.of());

        assertEquals(
                List.of(
                        "machine bench does not describe its system",
                        "machine bench does not name its Vulkan device",
                        "machine bench does not name its driver",
                        "machine bench has the Vulkan API version \"1.3\", expected major.minor.patch"),
                MachineRegistryRules.findViolations(registryOf(incomplete)));
    }

    @Test
    void rejectsAnEvidenceListedTwice() {
        MachineRegistry registry = registryOf(machine(
                "bench",
                SystemFamily.LINUX,
                DeviceType.DISCRETE_GPU,
                Evidence.NATIVE_PRESENTATION,
                Evidence.NATIVE_PRESENTATION));

        assertEquals(List.of("machine bench lists an evidence twice"), MachineRegistryRules.findViolations(registry));
    }
}
