package io.github.luasgraphicss.minemod.verification.differential.provider;

import io.github.luasgraphicss.minemod.compatibility.natives.binding.NativeBinding;
import io.github.luasgraphicss.minemod.compatibility.provider.capability.ContextProfile;
import io.github.luasgraphicss.minemod.compatibility.provider.load.ProviderLoadException;
import io.github.luasgraphicss.minemod.compatibility.runtime.fixtures.ProviderTestEnvironment;
import io.github.luasgraphicss.minemod.compatibility.runtime.fixtures.StagedProvider;
import io.github.luasgraphicss.minemod.compatibility.runtime.provider.ProviderBundle;
import io.github.luasgraphicss.minemod.compatibility.runtime.provider.ProviderRejectedException;
import io.github.luasgraphicss.minemod.verification.differential.ComparedProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

public final class SystemProvider implements ComparedProvider, AutoCloseable {
    private static final String STAGING_PREFIX = "minemod-provider";

    private final Path systemLibraries;
    private final Path staged;
    private final ProviderBundle bundle;

    private SystemProvider(Path systemLibraries, Path staged, ProviderBundle bundle) {
        this.systemLibraries = systemLibraries;
        this.staged = staged;
        this.bundle = bundle;
    }

    public static SystemProvider stage(Path systemLibraries, Path natives) throws IOException {
        if (!Files.isRegularFile(natives)) {
            throw new NoSuchFileException(natives.toString(), null, "the JNI library of the provider is not a file");
        }
        NativeBinding.load(natives);
        Path staged = Files.createTempDirectory(STAGING_PREFIX);
        try {
            return new SystemProvider(systemLibraries, staged, StagedProvider.stage(systemLibraries, staged));
        } catch (IOException | RuntimeException failure) {
            discard(staged);
            throw failure;
        }
    }

    @Override
    public ProviderComparison compareOn(ContextProfile profile) throws IOException {
        try {
            return new ProviderComparison(ProviderTestEnvironment.open(bundle, profile));
        } catch (ProviderLoadException | ProviderRejectedException failure) {
            throw new IOException(
                    "the provider staged from " + systemLibraries + " opened no " + profile + " context: "
                            + failure.getMessage(),
                    failure);
        }
    }

    @Override
    public void close() throws IOException {
        discard(staged);
    }

    private static void discard(Path directory) throws IOException {
        try (Stream<Path> entries = Files.walk(directory)) {
            for (Path entry : entries.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(entry);
            }
        }
    }
}
