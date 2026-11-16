package io.github.luasgraphicss.minemod.verification.layer;

import io.github.luasgraphicss.minemod.compatibility.dispatch.catalog.CallCatalog;
import io.github.luasgraphicss.minemod.compatibility.dispatch.catalog.CallDescription;
import io.github.luasgraphicss.minemod.compatibility.dispatch.catalog.Interception;
import io.github.luasgraphicss.minemod.verification.check.Check;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class CallCatalogCheck implements Check {
    @Override
    public String name() {
        return "call-catalog";
    }

    @Override
    public List<String> findViolations(Path repositoryRoot) {
        CallCatalog catalog;
        try {
            catalog = CallCatalog.standard();
        } catch (ExceptionInInitializerError | IllegalArgumentException unreadable) {
            return List.of("the call catalog cannot be read: " + unreadable);
        }
        List<String> violations = new ArrayList<>();
        for (String extension : catalog.extensions().extensions()) {
            for (String function : catalog.extensions().functionsOf(extension)) {
                if (catalog.describe(function).isEmpty()) {
                    violations.add(extension + " lists " + function + ", which no call description covers");
                }
            }
        }
        for (CallDescription description : catalog.descriptions()) {
            if (description.family().isLegacy() && description.interception() == Interception.CAPABILITIES) {
                violations.add(description.function() + " answers capabilities but only exists in legacy contexts");
            }
        }
        return violations;
    }
}
