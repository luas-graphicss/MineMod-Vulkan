package io.github.luasgraphicss.minemod.verification.differential.simulated;

import io.github.luasgraphicss.minemod.compatibility.provider.capability.ContextProfile;
import io.github.luasgraphicss.minemod.verification.differential.Divergence;
import io.github.luasgraphicss.minemod.verification.differential.ScriptComparison;
import io.github.luasgraphicss.minemod.verification.differential.ScriptPlayer;
import io.github.luasgraphicss.minemod.verification.differential.ScriptStep;
import io.github.luasgraphicss.minemod.verification.scenario.simulation.DirectDriver;
import io.github.luasgraphicss.minemod.verification.scenario.simulation.SimulatedLayer;
import java.util.List;
import java.util.Optional;

public final class SimulatedComparison implements ScriptComparison {
    public static final String PROVIDER = "the simulated provider";

    private final ContextProfile layerProfile;
    private final ContextProfile referenceProfile;

    public SimulatedComparison(ContextProfile layerProfile, ContextProfile referenceProfile) {
        this.layerProfile = layerProfile;
        this.referenceProfile = referenceProfile;
    }

    public static SimulatedComparison ofProfile(ContextProfile profile) {
        return new SimulatedComparison(profile, profile);
    }

    @Override
    public String provider() {
        return PROVIDER;
    }

    @Override
    public Optional<String> firstDifference(List<ScriptStep> script) {
        SimulatedLayer layer = new SimulatedLayer(layerProfile);
        layer.beginFrame();
        layer.enterCallback();
        List<String> layerObservations = new ScriptPlayer(new SimulatedTarget(layer)).play(script);
        layer.exitCallback();
        layer.present();

        DirectDriver reference = new DirectDriver(referenceProfile);
        List<String> referenceObservations = new ScriptPlayer(new SimulatedTarget(reference)).play(script);

        Optional<String> observed = Divergence.first("observation", referenceObservations, layerObservations);
        if (observed.isPresent()) {
            return observed;
        }
        Optional<String> called = Divergence.first("provider call", reference.providerCalls(), layer.providerCalls());
        if (called.isPresent()) {
            return called;
        }
        return layer.ownershipViolations().stream().findFirst();
    }

    @Override
    public void close() {}
}
