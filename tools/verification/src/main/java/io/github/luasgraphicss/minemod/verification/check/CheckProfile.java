package io.github.luasgraphicss.minemod.verification.check;

import io.github.luasgraphicss.minemod.verification.adapters.AdapterModulesCheck;
import io.github.luasgraphicss.minemod.verification.compatibility.CompatibilityCasesCheck;
import io.github.luasgraphicss.minemod.verification.ecosystem.EcosystemScenariosCheck;
import io.github.luasgraphicss.minemod.verification.frame.FramePlansCheck;
import io.github.luasgraphicss.minemod.verification.hooks.PresentationHooksCheck;
import io.github.luasgraphicss.minemod.verification.layer.CallCatalogCheck;
import io.github.luasgraphicss.minemod.verification.layer.DifferentialCheck;
import io.github.luasgraphicss.minemod.verification.layer.ScenariosCheck;
import io.github.luasgraphicss.minemod.verification.machines.HardwareEvidenceCheck;
import io.github.luasgraphicss.minemod.verification.machines.MachineRegistryCheck;
import io.github.luasgraphicss.minemod.verification.targets.TargetMatrixCheck;
import java.util.List;
import java.util.Optional;

public record CheckProfile(String name, List<Check> checks) {
    private static final List<CheckProfile> PROFILES = List.of(
            new CheckProfile("foundation", List.of(new TargetMatrixCheck(), new MachineRegistryCheck())),
            new CheckProfile("hardware", List.of(new MachineRegistryCheck(), new HardwareEvidenceCheck())),
            new CheckProfile(
                    "adapters",
                    List.of(
                            new TargetMatrixCheck(),
                            new PresentationHooksCheck(),
                            new AdapterModulesCheck(),
                            new FramePlansCheck())),
            new CheckProfile(
                    "ecosystem",
                    List.of(
                            new TargetMatrixCheck(),
                            new PresentationHooksCheck(),
                            new CompatibilityCasesCheck(),
                            new EcosystemScenariosCheck())),
            new CheckProfile(
                    "compatibility", List.of(new CallCatalogCheck(), new ScenariosCheck(), new DifferentialCheck())));

    public static Optional<CheckProfile> named(String name) {
        return PROFILES.stream().filter(profile -> profile.name().equals(name)).findFirst();
    }
}
