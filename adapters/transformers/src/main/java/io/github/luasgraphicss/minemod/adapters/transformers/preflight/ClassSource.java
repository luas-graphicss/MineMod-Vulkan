package io.github.luasgraphicss.minemod.adapters.transformers.preflight;

import java.io.IOException;
import java.util.Optional;

public interface ClassSource {
    Optional<byte[]> classFile(String internalName) throws IOException;
}
