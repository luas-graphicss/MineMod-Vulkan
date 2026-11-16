package io.github.luasgraphicss.minemod.client.shaders;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

final class IncludeCollector {
    static final int MAX_INCLUDES = 64;
    static final int MAX_DEPTH = 8;

    private final ShaderFiles files;
    private final Map<String, String> texts = new LinkedHashMap<>();
    private boolean replacesBuiltIn;

    IncludeCollector(ShaderFiles files) {
        this.files = files;
    }

    void collectFrom(String source) {
        collect(source, 0);
    }

    Map<String, String> texts() {
        return texts;
    }

    boolean replacesBuiltIn() {
        return replacesBuiltIn;
    }

    private void collect(String source, int depth) {
        if (depth == MAX_DEPTH) {
            return;
        }
        for (String key : ShaderImports.keysIn(source)) {
            if (texts.containsKey(key) || texts.size() == MAX_INCLUDES) {
                continue;
            }
            Optional<ShaderFile> imported = ShaderImports.pathOf(key).flatMap(files::open);
            if (!imported.isPresent()) {
                continue;
            }
            texts.put(key, imported.get().text());
            replacesBuiltIn |= imported.get().replacesBuiltIn();
            collect(imported.get().text(), depth + 1);
        }
    }
}
