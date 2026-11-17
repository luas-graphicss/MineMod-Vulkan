package io.github.luasgraphicss.minemod.adapters.shaderpacks.uniforms;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeMap;

public final class StandardUniforms {
    private static final String BUNDLED = "standard-uniforms.properties";

    private final Map<String, String> typesByName;

    public StandardUniforms(Map<String, String> typesByName) {
        this.typesByName = Collections.unmodifiableMap(new TreeMap<>(typesByName));
    }

    public static StandardUniforms bundled() {
        Properties uniforms = new Properties();
        try (InputStream resource = StandardUniforms.class.getResourceAsStream(BUNDLED)) {
            if (resource == null) {
                throw new IllegalStateException("the artifact carries no " + BUNDLED);
            }
            uniforms.load(resource);
        } catch (IOException exception) {
            throw new UncheckedIOException("cannot read " + BUNDLED, exception);
        }
        Map<String, String> typesByName = new TreeMap<>();
        for (String name : uniforms.stringPropertyNames()) {
            typesByName.put(name, uniforms.getProperty(name).trim());
        }
        return new StandardUniforms(typesByName);
    }

    public Optional<String> typeOf(String uniform) {
        return Optional.ofNullable(typesByName.get(uniform));
    }
}
