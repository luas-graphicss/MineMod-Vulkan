package io.github.luasgraphicss.minemod.compatibility.provider.load;

import java.nio.file.Path;

public interface LibraryOpener {
    long FAILED = 0;

    long open(Path library);

    void close(long handle);
}
