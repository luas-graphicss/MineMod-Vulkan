package io.github.luasgraphicss.minemod.verification.machines.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public record MachineRegistry(int schemaVersion, List<ReferenceMachine> machines) {
    public static final int SCHEMA_VERSION = 1;

    public MachineRegistry withMachine(ReferenceMachine recorded) {
        List<ReferenceMachine> updated = new ArrayList<>(machines);
        updated.removeIf(machine -> machine.id().equals(recorded.id()));
        updated.add(recorded);
        updated.sort(Comparator.comparing(ReferenceMachine::id));
        return new MachineRegistry(schemaVersion, List.copyOf(updated));
    }
}
