package io.github.luasgraphicss.minemod.compatibility.layer.route;

import io.github.luasgraphicss.minemod.compatibility.layer.capability.ExposedExtensions;
import io.github.luasgraphicss.minemod.compatibility.provider.capability.ContextProfile;
import io.github.luasgraphicss.minemod.compatibility.state.error.ErrorLedger;
import io.github.luasgraphicss.minemod.compatibility.translation.provider.ProviderCalls;

final class CapabilityRoutes {
    private static final String INDEXED_STRING = "glGetStringi";
    private static final int GL_EXTENSIONS = 0x1F03;
    private static final int GL_INVALID_VALUE = 0x0501;
    private static final long NO_STRING = 0;
    private final ExposedExtensions exposed;
    private final ContextProfile profile;
    private final ProviderCalls provider;
    private final ErrorLedger errors;

    CapabilityRoutes(ExposedExtensions exposed, ContextProfile profile, ProviderCalls provider, ErrorLedger errors) {
        this.exposed = exposed;
        this.profile = profile;
        this.provider = provider;
        this.errors = errors;
    }

    long call(String coreFunction, String function, long[] words) {
        if (words[0] != GL_EXTENSIONS) {
            return provider.invoke(function, words);
        }
        if (coreFunction.equals(INDEXED_STRING)) {
            return extensionAt((int) words[1]);
        }
        return profile == ContextProfile.CORE ? provider.invoke(function, words) : exposed.joined();
    }

    private long extensionAt(int index) {
        if (index < 0 || index >= exposed.count()) {
            errors.raise(GL_INVALID_VALUE);
            return NO_STRING;
        }
        return exposed.nameAt(index);
    }
}
