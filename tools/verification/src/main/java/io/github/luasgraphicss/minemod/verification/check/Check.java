package io.github.luasgraphicss.minemod.verification.check;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface Check {
    String name();

    List<String> findViolations(Path repositoryRoot) throws IOException;
}
