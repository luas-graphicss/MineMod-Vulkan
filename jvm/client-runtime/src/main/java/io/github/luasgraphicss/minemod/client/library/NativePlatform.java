package io.github.luasgraphicss.minemod.client.library;

import java.util.Locale;

public final class NativePlatform {
    private static final String BUNDLE_ROOT = "natives/";
    private static final String DIGEST_SUFFIX = ".sha256";
    private static final String RENDERER = "minemod_jni";
    private static final String OPENGL_PROVIDER = "minemod_gl_provider_jni";

    private final String name;
    private final String libraryFile;

    private NativePlatform(String name, String libraryFile) {
        this.name = name;
        this.libraryFile = libraryFile;
    }

    public static NativePlatform current() {
        return of(System.getProperty("os.name"), System.getProperty("os.arch"));
    }

    public static NativePlatform of(String systemName, String architectureName) {
        String system = systemName.toLowerCase(Locale.ROOT);
        String architecture = architecture(architectureName);
        if (system.startsWith("windows")) {
            return new NativePlatform("windows-" + architecture, RENDERER + ".dll");
        }
        if (system.startsWith("linux")) {
            return new NativePlatform("linux-" + architecture, "lib" + RENDERER + ".so");
        }
        throw new NativeLibraryUnavailable("the native renderer is not built for " + systemName);
    }

    public NativePlatform openGlProvider() {
        return new NativePlatform(name, libraryFile.replace(RENDERER, OPENGL_PROVIDER));
    }

    public String libraryFile() {
        return libraryFile;
    }

    public String libraryResource() {
        return BUNDLE_ROOT + name + "/" + libraryFile;
    }

    public String digestResource() {
        return libraryResource() + DIGEST_SUFFIX;
    }

    private static String architecture(String reported) {
        String architecture = reported.toLowerCase(Locale.ROOT);
        if (architecture.equals("amd64") || architecture.equals("x86_64")) {
            return "x86_64";
        }
        if (architecture.equals("aarch64") || architecture.equals("arm64")) {
            return "aarch64";
        }
        throw new NativeLibraryUnavailable("the native renderer is not built for the " + reported + " architecture");
    }
}
