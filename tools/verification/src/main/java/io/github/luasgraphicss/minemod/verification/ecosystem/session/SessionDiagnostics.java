package io.github.luasgraphicss.minemod.verification.ecosystem.session;

import io.github.luasgraphicss.minemod.adapters.configuration.diagnostics.DiagnosticReport;
import io.github.luasgraphicss.minemod.adapters.configuration.settings.RendererSettings;
import io.github.luasgraphicss.minemod.adapters.ownership.negotiation.OwnershipDecision;
import io.github.luasgraphicss.minemod.compatibility.bridges.contract.ContractVerdict;
import io.github.luasgraphicss.minemod.verification.ecosystem.pack.Modpack;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class SessionDiagnostics {
    private SessionDiagnostics() {}

    static List<String> describe(
            Modpack pack, Launch launch, OwnershipDecision ownership, Map<String, ContractVerdict> bridgeVerdicts) {
        List<String> renderers = new ArrayList<>();
        ownership.bridgedStages().forEach((stage, renderer) -> renderers.add(stage + " bridged for " + renderer));
        ownership.conflicts().forEach(conflict -> renderers.add(conflict.toString()));
        List<String> bridges = new ArrayList<>();
        bridgeVerdicts.forEach((bridge, verdict) -> bridges.add(bridge + " " + verdict.status()));
        return DiagnosticReport.forTarget(pack.release(), pack.loader().name().toLowerCase(Locale.ROOT))
                .section("settings", launch.settings().describe(RendererSettings.all()))
                .section("settings rejected", launch.settings().rejected())
                .section("renderers", renderers)
                .section("bridges", bridges)
                .section("recovery", List.of(launch.recovery()))
                .build()
                .lines();
    }
}
