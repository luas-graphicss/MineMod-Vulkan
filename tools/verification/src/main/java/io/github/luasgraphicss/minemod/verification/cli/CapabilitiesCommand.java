package io.github.luasgraphicss.minemod.verification.cli;

import io.github.luasgraphicss.minemod.compatibility.dispatch.catalog.CallCatalog;
import io.github.luasgraphicss.minemod.compatibility.provider.capability.ContextProfile;
import io.github.luasgraphicss.minemod.verification.capability.CapabilityMatrix;
import io.github.luasgraphicss.minemod.verification.capability.ReportedConditions;
import io.github.luasgraphicss.minemod.verification.differential.DifferentialSuite;
import io.github.luasgraphicss.minemod.verification.scenario.simulation.SimulatedLayer;
import io.github.luasgraphicss.minemod.verification.scenario.synchronous.NativeSpanFrame;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class CapabilitiesCommand {
    private static final String INDENT = "  ";

    private CapabilitiesCommand() {}

    static int run(Invocation invocation, PrintStream output) {
        invocation.allowOnly(Set.of("context", "root"));
        List<ContextProfile> profiles = invocation
                .option("context")
                .map(name -> List.of(profileNamed(name)))
                .orElseGet(() -> List.of(ContextProfile.values()));
        CallCatalog catalog = CallCatalog.standard();
        output.println("capabilities against the " + DifferentialSuite.PROVIDER
                + " provider, which exports every described function");
        for (ContextProfile profile : profiles) {
            output.println(nameOf(profile) + " context:");
            CapabilityMatrix.of(catalog, profile).rows().forEach(row -> output.println(INDENT + row));
        }
        output.println("reported while a frame runs:");
        conditionsOfAProbeFrame().forEach(condition -> output.println(INDENT + condition));
        return VerificationCli.EXIT_OK;
    }

    private static List<String> conditionsOfAProbeFrame() {
        SimulatedLayer layer = new SimulatedLayer(ContextProfile.CORE);
        NativeSpanFrame.play(layer);
        return ReportedConditions.of(layer.reports());
    }

    private static ContextProfile profileNamed(String name) {
        return Arrays.stream(ContextProfile.values())
                .filter(profile -> nameOf(profile).equals(name))
                .findFirst()
                .orElseThrow(() -> new UsageException("unknown context: " + name));
    }

    private static String nameOf(ContextProfile profile) {
        return profile.name().toLowerCase(Locale.ROOT);
    }
}
