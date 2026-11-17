package io.github.luasgraphicss.minemod.adapters.loaders.launchwrapper;

import io.github.luasgraphicss.minemod.adapters.loaders.launch.apply.ClassFileApplication;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;

public final class PinnedCallsTweaker implements ITweaker {
    private static final String NO_LAUNCH_TARGET = "";

    @Override
    public void acceptOptions(List<String> arguments, File gameDirectory, File assetsDirectory, String profile) {}

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        ClassFileApplication application = PinnedCallsTransformer.applicationOf(PinnedCallsTweaker.class);
        try {
            application.requireApplicableTo(className -> Optional.ofNullable(classLoader.getClassBytes(className)));
        } catch (IOException unreadable) {
            throw new UncheckedIOException("the classes of the game cannot be read", unreadable);
        }
        classLoader.registerTransformer(PinnedCallsTransformer.class.getName());
    }

    @Override
    public String getLaunchTarget() {
        return NO_LAUNCH_TARGET;
    }

    @Override
    public String[] getLaunchArguments() {
        return new String[0];
    }
}
