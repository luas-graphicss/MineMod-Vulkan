package io.github.luasgraphicss.minemod.adapters.presentation.renderer.session;

import io.github.luasgraphicss.minemod.adapters.presentation.renderer.WindowRenderer;
import io.github.luasgraphicss.minemod.adapters.presentation.renderer.WindowRenderers;
import io.github.luasgraphicss.minemod.client.library.BundledLibrary;
import io.github.luasgraphicss.minemod.client.library.NativePlatform;
import io.github.luasgraphicss.minemod.client.session.ClientRenderer;
import io.github.luasgraphicss.minemod.client.session.DeviceValidation;
import io.github.luasgraphicss.minemod.client.session.SceneAttachments;
import io.github.luasgraphicss.minemod.client.window.WindowSurface;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.function.Function;

public final class BundledRenderers implements WindowRenderers {
    private static final String WORKING_DIRECTORY = "user.dir";
    private static final String NATIVE_CACHE = "minemod-vulkan/natives";
    private static final String VALIDATION_PROPERTY = "minemod.validation";

    private final ClassLoader bundle;
    private final Path cacheDirectory;
    private final Function<ClientRenderer, WindowRenderer> presenting;

    public BundledRenderers(ClassLoader bundle, Path cacheDirectory) {
        this(bundle, cacheDirectory, ClearedFrameRenderer::new);
    }

    public BundledRenderers(
            ClassLoader bundle, Path cacheDirectory, Function<ClientRenderer, WindowRenderer> presenting) {
        this.bundle = Objects.requireNonNull(bundle, "bundle");
        this.cacheDirectory = Objects.requireNonNull(cacheDirectory, "cacheDirectory");
        this.presenting = Objects.requireNonNull(presenting, "presenting");
    }

    public static BundledRenderers inGameDirectory(ClassLoader bundle) {
        return inGameDirectory(bundle, ClearedFrameRenderer::new);
    }

    public static BundledRenderers inGameDirectory(
            ClassLoader bundle, Function<ClientRenderer, WindowRenderer> presenting) {
        return new BundledRenderers(
                bundle, Paths.get(System.getProperty(WORKING_DIRECTORY)).resolve(NATIVE_CACHE), presenting);
    }

    @Override
    public WindowRenderer start(WindowSurface surface) {
        Path library = new BundledLibrary(bundle, NativePlatform.current()).extractInto(cacheDirectory);
        DeviceValidation validation =
                Boolean.getBoolean(VALIDATION_PROPERTY) ? DeviceValidation.ENABLED : DeviceValidation.DISABLED;
        return presenting.apply(ClientRenderer.start(library, surface, validation, SceneAttachments.PRIVATE));
    }
}
