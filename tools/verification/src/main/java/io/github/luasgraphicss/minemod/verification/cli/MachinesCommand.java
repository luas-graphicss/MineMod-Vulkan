package io.github.luasgraphicss.minemod.verification.cli;

import io.github.luasgraphicss.minemod.verification.machines.model.MachineRegistry;
import io.github.luasgraphicss.minemod.verification.machines.model.ReferenceMachine;
import io.github.luasgraphicss.minemod.verification.machines.model.ReferenceMachine.Evidence;
import io.github.luasgraphicss.minemod.verification.machines.model.ReferenceMachine.HostSystem;
import io.github.luasgraphicss.minemod.verification.machines.policy.MachineRegistryRules;
import io.github.luasgraphicss.minemod.verification.machines.probe.HarnessProcess;
import io.github.luasgraphicss.minemod.verification.machines.probe.HarnessReport;
import io.github.luasgraphicss.minemod.verification.machines.probe.HostSystems;
import io.github.luasgraphicss.minemod.verification.machines.probe.SuiteResults;
import io.github.luasgraphicss.minemod.verification.machines.probe.SuiteResults.SuiteOutcome;
import io.github.luasgraphicss.minemod.verification.machines.record.MachineRecorder;
import io.github.luasgraphicss.minemod.verification.machines.record.MachineRecorder.Recording;
import io.github.luasgraphicss.minemod.verification.machines.storage.MachineRegistryFile;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

final class MachinesCommand {
    private static final String ID = "id";
    private static final String HARNESS = "harness";
    private static final String RESULTS = "results";
    private static final String SYSTEM = "system";

    private MachinesCommand() {}

    static int run(Invocation invocation, Path repositoryRoot, PrintStream output)
            throws IOException, InterruptedException {
        invocation.allowOnly(Set.of(ID, HARNESS, RESULTS, SYSTEM, "root"));
        String id = invocation.requiredOption(ID);
        Path harness = repositoryRoot.resolve(invocation.requiredOption(HARNESS));

        Recording recording = MachineRecorder.record(
                id,
                systemOf(invocation),
                HarnessReport.parse(HarnessProcess.run(harness)),
                suitesOf(invocation, repositoryRoot));
        MachineRegistry registry = MachineRegistryFile.readFrom(repositoryRoot).withMachine(recording.machine());
        List<String> violations = MachineRegistryRules.findViolations(registry);
        if (!violations.isEmpty()) {
            violations.forEach(violation -> output.println("  - " + violation));
            output.println("machine " + id + ": not recorded, " + violations.size() + " violation(s)");
            return VerificationCli.EXIT_VIOLATIONS;
        }

        MachineRegistryFile.writeTo(repositoryRoot, registry);
        recording.unproven().forEach(shortfall -> output.println("  not recorded, " + shortfall));
        output.println("machine " + id + ": " + summaryOf(recording.machine()));
        return VerificationCli.EXIT_OK;
    }

    private static HostSystem systemOf(Invocation invocation) throws IOException {
        Optional<String> description = invocation.option(SYSTEM);
        return description.isPresent() ? HostSystems.currentDescribedAs(description.get()) : HostSystems.current();
    }

    private static Map<String, SuiteOutcome> suitesOf(Invocation invocation, Path repositoryRoot) throws IOException {
        Optional<String> results = invocation.option(RESULTS);
        return results.isPresent() ? SuiteResults.readFrom(repositoryRoot.resolve(results.get())) : Map.of();
    }

    private static String summaryOf(ReferenceMachine machine) {
        String evidence = machine.evidence().isEmpty()
                ? "no evidence"
                : machine.evidence().stream().map(Evidence::label).collect(Collectors.joining(", "));
        return machine.device().name() + " (" + machine.device().type().label() + ") on "
                + machine.system().family().label() + ", " + evidence;
    }
}
