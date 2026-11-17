package io.github.luasgraphicss.minemod.verification.machines.fixtures;

import io.github.luasgraphicss.minemod.verification.machines.model.MachineRegistry;
import io.github.luasgraphicss.minemod.verification.machines.model.ReferenceMachine;
import io.github.luasgraphicss.minemod.verification.machines.model.ReferenceMachine.DeviceType;
import io.github.luasgraphicss.minemod.verification.machines.model.ReferenceMachine.Evidence;
import io.github.luasgraphicss.minemod.verification.machines.model.ReferenceMachine.HostSystem;
import io.github.luasgraphicss.minemod.verification.machines.model.ReferenceMachine.SystemFamily;
import io.github.luasgraphicss.minemod.verification.machines.model.ReferenceMachine.VulkanDevice;
import java.util.List;

public final class SampleMachines {
    public static final VulkanDevice LAVAPIPE = new VulkanDevice(
            "llvmpipe (LLVM 15.0.7, 256 bits)", DeviceType.CPU, "llvmpipe Mesa 24.2.6 (LLVM 15.0.7)", "1.3.289");

    private SampleMachines() {}

    public static ReferenceMachine machine(String id, SystemFamily family, DeviceType type, Evidence... evidence) {
        return new ReferenceMachine(
                id,
                new HostSystem(family, "test bench"),
                new VulkanDevice("test device", type, "test driver 1.0", "1.3.289"),
                List.of(evidence));
    }

    public static MachineRegistry registryOf(ReferenceMachine... machines) {
        return new MachineRegistry(MachineRegistry.SCHEMA_VERSION, List.of(machines));
    }
}
