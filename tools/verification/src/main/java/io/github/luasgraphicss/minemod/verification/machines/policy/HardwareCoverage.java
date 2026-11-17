package io.github.luasgraphicss.minemod.verification.machines.policy;

import io.github.luasgraphicss.minemod.verification.machines.model.MachineRegistry;
import io.github.luasgraphicss.minemod.verification.machines.model.ReferenceMachine;
import io.github.luasgraphicss.minemod.verification.machines.model.ReferenceMachine.DeviceType;
import io.github.luasgraphicss.minemod.verification.machines.model.ReferenceMachine.Evidence;
import io.github.luasgraphicss.minemod.verification.machines.model.ReferenceMachine.SystemFamily;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class HardwareCoverage {
    private static final Set<DeviceType> REAL_GPUS = EnumSet.of(DeviceType.INTEGRATED_GPU, DeviceType.DISCRETE_GPU);

    private HardwareCoverage() {}

    public static List<String> findGaps(MachineRegistry registry) {
        List<String> gaps = new ArrayList<>();
        for (SystemFamily family : SystemFamily.values()) {
            Set<Evidence> missing = EnumSet.complementOf(recordedOnRealGpus(registry, family));
            if (!missing.isEmpty()) {
                gaps.add("no machine with a real GPU on " + family.label() + " recorded "
                        + missing.stream().map(Evidence::label).collect(Collectors.joining(", ")));
            }
        }
        return gaps;
    }

    private static EnumSet<Evidence> recordedOnRealGpus(MachineRegistry registry, SystemFamily family) {
        EnumSet<Evidence> recorded = EnumSet.noneOf(Evidence.class);
        for (ReferenceMachine machine : registry.machines()) {
            boolean runsOnRealGpu = REAL_GPUS.contains(machine.device().type());
            if (runsOnRealGpu && machine.system().family() == family) {
                recorded.addAll(machine.evidence());
            }
        }
        return recorded;
    }
}
