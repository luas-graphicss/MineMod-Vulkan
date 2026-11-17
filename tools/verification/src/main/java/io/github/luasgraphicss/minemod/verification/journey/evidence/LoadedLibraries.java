package io.github.luasgraphicss.minemod.verification.journey.evidence;

import java.util.List;
import java.util.regex.Pattern;

public record LoadedLibraries(List<String> names) {
    public static final String NATIVE_RENDERER = "libminemod_jni.so";
    public static final String OPENGL_PROVIDER = "libminemod_gl_provider_jni.so";

    private static final Pattern OPENGL_ENTRY_POINT =
            Pattern.compile("lib(GL|GLX|OpenGL|EGL|GLESv1_CM|GLESv2)\\.so(\\.\\d+)*");
    private static final String ZINK = "zink";
    private static final String VULKAN_LOADER = "libvulkan.so";

    public LoadedLibraries {
        names = List.copyOf(names);
    }

    public List<String> openGl() {
        return names.stream()
                .filter(name -> OPENGL_ENTRY_POINT.matcher(name).matches() || name.contains(ZINK))
                .toList();
    }

    public boolean hasVulkanLoader() {
        return names.stream().anyMatch(name -> name.startsWith(VULKAN_LOADER));
    }

    public boolean has(String library) {
        return names.contains(library);
    }
}
