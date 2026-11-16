package io.github.luasgraphicss.minemod.client.opengl.programs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class ShaderPrograms {
    public static final int NO_LOCATION = -1;
    private static final Pattern LINE_END = Pattern.compile("\\R");
    private static final String UNIFORM_DECLARATION = "uniform ";

    private final Map<Integer, String> sources = new HashMap<>();
    private final Map<Integer, List<Integer>> attachedShaders = new HashMap<>();
    private final Map<Integer, Map<String, Integer>> locations = new HashMap<>();
    private int lastName;

    public int createShader() {
        return ++lastName;
    }

    public int createProgram() {
        int program = ++lastName;
        attachedShaders.put(program, new ArrayList<>());
        locations.put(program, new HashMap<>());
        return program;
    }

    public void source(int shader, List<String> parts) {
        sources.put(shader, String.join("", parts));
    }

    public void attach(int program, int shader) {
        List<Integer> shaders = attachedShaders.get(program);
        if (shaders != null && !shaders.contains(shader)) {
            shaders.add(shader);
        }
    }

    public int uniformLocation(int program, CharSequence uniform) {
        Map<String, Integer> known = locations.get(program);
        if (known == null) {
            return NO_LOCATION;
        }
        String name = uniform.toString();
        Integer location = known.get(name);
        if (location == null) {
            location = isUsed(program, name) ? known.size() : NO_LOCATION;
            if (location != NO_LOCATION) {
                known.put(name, location);
            }
        }
        return location;
    }

    public void deleteShader(int shader) {
        sources.remove(shader);
    }

    public void deleteProgram(int program) {
        attachedShaders.remove(program);
        locations.remove(program);
    }

    private boolean isUsed(int program, String name) {
        Pattern use = Pattern.compile("\\b" + Pattern.quote(name) + "\\b");
        for (int shader : attachedShaders.get(program)) {
            String source = sources.get(shader);
            if (source != null && usesOutsideDeclarations(source, use)) {
                return true;
            }
        }
        return false;
    }

    private static boolean usesOutsideDeclarations(String source, Pattern use) {
        for (String line : LINE_END.split(source)) {
            if (!line.trim().startsWith(UNIFORM_DECLARATION) && use.matcher(line).find()) {
                return true;
            }
        }
        return false;
    }
}
