package io.github.luasgraphicss.minemod.verification.differential;

import java.util.List;
import java.util.Optional;

public interface ScriptComparison extends AutoCloseable {
    String provider();

    Optional<String> firstDifference(List<ScriptStep> script);

    @Override
    void close();
}
