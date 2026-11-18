package io.github.luasgraphicss.minemod.adapters.loaders.launchwrapper;

import io.github.luasgraphicss.minemod.adapters.frame.display.DisplayFrameOwner;
import io.github.luasgraphicss.minemod.adapters.loaders.launch.apply.ClassFileApplications;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;

public final class PinnedCallsTweaker implements ITweaker {
    private static final String NO_LAUNCH_TARGET = "";
    private static final String ADAPTER_PACKAGES = "io.github.luasgraphicss.minemod.";

    @Override
    public void acceptOptions(List<String> arguments, File gameDirectory, File assetsDirectory, String profile) {}

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        ClassFileApplications applications = PinnedCallsTransformer.applicationsOf(PinnedCallsTweaker.class);
        try {
            applications.requireApplicableTo(className -> Optional.ofNullable(classLoader.getClassBytes(className)));
        } catch (IOException unreadable) {
            throw new UncheckedIOException("the classes of the game cannot be read", unreadable);
        }
        classLoader.addClassLoaderExclusion(ADAPTER_PACKAGES);
        DisplayFrameOwner.installFrom(PinnedCallsTweaker.class.getClassLoader());
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
