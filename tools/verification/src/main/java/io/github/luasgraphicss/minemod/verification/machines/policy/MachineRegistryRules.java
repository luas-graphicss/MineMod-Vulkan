package io.github.luasgraphicss.minemod.verification.machines.policy;

import io.github.luasgraphicss.minemod.verification.machines.model.MachineRegistry;
import io.github.luasgraphicss.minemod.verification.machines.model.ReferenceMachine;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public final class MachineRegistryRules {
    private static final Pattern MACHINE_ID = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");
    private static final Pattern API_VERSION = Pattern.compile("[0-9]+\\.[0-9]+\\.[0-9]+");

    private MachineRegistryRules() {}

    public static List<String> findViolations(MachineRegistry registry) {
        List<String> violations = new ArrayList<>();
        if (registry.schemaVersion() != MachineRegistry.SCHEMA_VERSION) {
            violations.add(
                    "schemaVersion is " + registry.schemaVersion() + ", expected " + MachineRegistry.SCHEMA_VERSION);
        }

        Set<String> seenIds = new HashSet<>();
        for (ReferenceMachine machine : registry.machines()) {
            if (!MACHINE_ID.matcher(machine.id()).matches()) {
                violations.add("machine id \"" + machine.id() + "\" is not lowercase words joined by hyphens");
                continue;
            }
            if (!seenIds.add(machine.id())) {
                violations.add("machine " + machine.id() + " is listed twice");
            }
            checkMachine(machine, violations);
        }
        return violations;
    }

    private static void checkMachine(ReferenceMachine machine, List<String> violations) {
        String subject = "machine " + machine.id();
        if (machine.system().description().isBlank()) {
            violations.add(subject + " does not describe its system");
        }
        if (machine.device().name().isBlank()) {
            violations.add(subject + " does not name its Vulkan device");
        }
        if (machine.device().driver().isBlank()) {
            violations.add(subject + " does not name its driver");
        }
        if (!API_VERSION.matcher(machine.device().vulkanApi()).matches()) {
            violations.add(subject + " has the Vulkan API version \"" + machine.device().vulkanApi()
                    + "\", expected major.minor.patch");
        }
        if (new HashSet<>(machine.evidence()).size() != machine.evidence().size()) {
            violations.add(subject + " lists an evidence twice");
        }
    }
}
