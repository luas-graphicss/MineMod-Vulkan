package io.github.luasgraphicss.minemod.compatibility.contracts.report;

import java.util.Optional;

public interface OriginResolver {
    Optional<String> locationOf(String className);
}
