package io.github.luasgraphicss.minemod.verification.journey.run;

import io.github.luasgraphicss.minemod.verification.journey.instance.GameInstance;
import io.github.luasgraphicss.minemod.verification.journey.instance.InstanceExit;
import io.github.luasgraphicss.minemod.verification.journey.plan.JourneyPlan;
import io.github.luasgraphicss.minemod.verification.journey.record.JourneyRecord;
import java.nio.file.Path;

public record JourneyEvidence(
        GameInstance instance, JourneyPlan plan, JourneyRecord record, InstanceExit exit, Path directory) {

    public String subject() {
        return instance.label() + ": ";
    }
}
