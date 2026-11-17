package io.github.luasgraphicss.minemod.adapters.shaderpacks.directives;

import io.github.luasgraphicss.minemod.adapters.shaderpacks.pack.ProgramSources;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.pack.ShaderPack;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.targets.SamplerNames;
import java.util.Collections;
import java.util.Map;
import java.util.OptionalInt;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PackSettings {
    private static final Pattern CONSTANT =
            Pattern.compile("\\bconst\\s+(?:int|float|bool)\\s+(\\w+)\\s*=\\s*([\\w.+-]+)\\s*;");
    private static final String FORMAT_SUFFIX = "Format";
    private static final String CLEAR_SUFFIX = "Clear";
    private static final String SHADOW_MAP_RESOLUTION = "shadowMapResolution";
    private static final int DEFAULT_SHADOW_MAP_RESOLUTION = 1024;

    private final Map<Integer, String> formatNames = new TreeMap<>();
    private final Map<Integer, Boolean> clears = new TreeMap<>();
    private String shadowMapResolution = Integer.toString(DEFAULT_SHADOW_MAP_RESOLUTION);

    private PackSettings() {}

    public static PackSettings of(ShaderPack pack) {
        PackSettings settings = new PackSettings();
        for (ProgramSources sources : pack.programs().values()) {
            settings.collect(sources.vertex());
            settings.collect(sources.fragment());
        }
        return settings;
    }

    public Map<Integer, String> formatNames() {
        return Collections.unmodifiableMap(formatNames);
    }

    public boolean clears(int colorTarget) {
        return clears.getOrDefault(colorTarget, Boolean.TRUE);
    }

    public String shadowMapResolution() {
        return shadowMapResolution;
    }

    private void collect(String source) {
        Matcher constant = CONSTANT.matcher(source);
        while (constant.find()) {
            String name = constant.group(1);
            String value = constant.group(2);
            if (name.equals(SHADOW_MAP_RESOLUTION)) {
                shadowMapResolution = value;
            } else if (name.endsWith(FORMAT_SUFFIX)) {
                colorTargetNamedBy(name, FORMAT_SUFFIX).ifPresent(target -> formatNames.put(target, value));
            } else if (name.endsWith(CLEAR_SUFFIX)) {
                colorTargetNamedBy(name, CLEAR_SUFFIX).ifPresent(target -> clears.put(target, Boolean.valueOf(value)));
            }
        }
    }

    private static OptionalInt colorTargetNamedBy(String constant, String suffix) {
        return SamplerNames.colorIndexOf(constant.substring(0, constant.length() - suffix.length()));
    }
}
