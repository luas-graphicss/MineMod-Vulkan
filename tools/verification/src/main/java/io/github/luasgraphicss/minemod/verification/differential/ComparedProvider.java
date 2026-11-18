package io.github.luasgraphicss.minemod.verification.differential;

import io.github.luasgraphicss.minemod.compatibility.provider.capability.ContextProfile;
import java.io.IOException;

@FunctionalInterface
public interface ComparedProvider {
    ScriptComparison compareOn(ContextProfile profile) throws IOException;
}
