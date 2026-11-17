package io.github.luasgraphicss.minemod.adapters.loaders.launch.plan;

import io.github.luasgraphicss.minemod.adapters.transformers.gate.TransformViolation;
import java.util.List;

public final class PlanRefusedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public PlanRefusedException(String release, List<TransformViolation> violations) {
        super("the calls pinned for Minecraft " + release + " are not the ones this game makes: "
                + describe(violations));
    }

    private static String describe(List<TransformViolation> violations) {
        StringBuilder described = new StringBuilder();
        for (TransformViolation violation : violations) {
            if (described.length() > 0) {
                described.append("; ");
            }
            described.append(violation.description());
        }
        return described.toString();
    }
}
