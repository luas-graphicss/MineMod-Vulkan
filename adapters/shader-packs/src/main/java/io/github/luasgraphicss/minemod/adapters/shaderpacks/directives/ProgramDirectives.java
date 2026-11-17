package io.github.luasgraphicss.minemod.adapters.shaderpacks.directives;

import io.github.luasgraphicss.minemod.adapters.shaderpacks.pack.ProgramSources;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ProgramDirectives {
    private static final Pattern DRAW_BUFFERS = Pattern.compile("/\\*\\s*DRAWBUFFERS:\\s*([0-9]+)\\s*\\*/");
    private static final Pattern RENDER_TARGETS =
            Pattern.compile("/\\*\\s*RENDERTARGETS:\\s*([0-9]+(?:\\s*,\\s*[0-9]+)*)\\s*\\*/");
    private static final Pattern UNIFORM =
            Pattern.compile("\\buniform\\s+(?:(?:lowp|mediump|highp)\\s+)?(\\w+)\\s+([^;]+);");
    private static final Pattern ARRAY_SUFFIX = Pattern.compile("\\[[^\\]]*\\]");
    private static final List<Integer> DEFAULT_DRAW_BUFFERS = Collections.singletonList(0);

    private final List<Integer> drawBuffers;
    private final Map<String, String> uniformTypes;

    private ProgramDirectives(List<Integer> drawBuffers, Map<String, String> uniformTypes) {
        this.drawBuffers = Collections.unmodifiableList(drawBuffers);
        this.uniformTypes = Collections.unmodifiableMap(uniformTypes);
    }

    public static ProgramDirectives of(ProgramSources sources) {
        Map<String, String> uniformTypes = new LinkedHashMap<>();
        collectUniforms(sources.vertex(), uniformTypes);
        collectUniforms(sources.fragment(), uniformTypes);
        return new ProgramDirectives(drawBuffersOf(sources.fragment()), uniformTypes);
    }

    public List<Integer> drawBuffers() {
        return drawBuffers;
    }

    public Map<String, String> uniformTypes() {
        return uniformTypes;
    }

    private static List<Integer> drawBuffersOf(String fragment) {
        Matcher renderTargets = RENDER_TARGETS.matcher(fragment);
        if (renderTargets.find()) {
            List<Integer> targets = new ArrayList<>();
            for (String index : renderTargets.group(1).split(",")) {
                targets.add(Integer.valueOf(index.trim()));
            }
            return targets;
        }
        Matcher drawBuffers = DRAW_BUFFERS.matcher(fragment);
        if (drawBuffers.find()) {
            List<Integer> targets = new ArrayList<>();
            for (char digit : drawBuffers.group(1).toCharArray()) {
                targets.add(Character.digit(digit, 10));
            }
            return targets;
        }
        return DEFAULT_DRAW_BUFFERS;
    }

    private static void collectUniforms(String source, Map<String, String> uniformTypes) {
        Matcher uniform = UNIFORM.matcher(GlslComments.strip(source));
        while (uniform.find()) {
            for (String declarator : uniform.group(2).split(",")) {
                uniformTypes.put(ARRAY_SUFFIX.matcher(declarator).replaceAll("").trim(), uniform.group(1));
            }
        }
    }
}
