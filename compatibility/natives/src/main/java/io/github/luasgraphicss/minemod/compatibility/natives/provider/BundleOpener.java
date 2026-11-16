package io.github.luasgraphicss.minemod.compatibility.natives.provider;

import io.github.luasgraphicss.minemod.compatibility.natives.binding.NativeBinding;
import io.github.luasgraphicss.minemod.compatibility.natives.binding.NativeProviderException;
import io.github.luasgraphicss.minemod.compatibility.provider.artifact.ArtifactDigest;
import io.github.luasgraphicss.minemod.compatibility.provider.load.LibraryOpener;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class BundleOpener implements LibraryOpener {
    private final Map<String, ArtifactDigest> dependencies;
    private final Map<String, String> driverSettings;

    public BundleOpener(Map<String, ArtifactDigest> dependenciesInLoadOrder, Map<String, String> driverSettings) {
        this.dependencies = new LinkedHashMap<>(dependenciesInLoadOrder);
        this.driverSettings = new LinkedHashMap<>(driverSettings);
    }

    @Override
    public long open(Path library) {
        Path directory = library.getParent();
        for (Map.Entry<String, ArtifactDigest> dependency : dependencies.entrySet()) {
            requirePinned(directory.resolve(dependency.getKey()), dependency.getValue());
        }
        return NativeBinding.openProvider(
                directory.toString(),
                dependencies.keySet().toArray(new String[0]),
                library.getFileName().toString(),
                driverSettings.keySet().toArray(new String[0]),
                driverSettings.values().toArray(new String[0]));
    }

    @Override
    public void close(long handle) {
        NativeBinding.closeProvider(handle);
    }

    private static void requirePinned(Path dependency, ArtifactDigest pinned) {
        try {
            if (!ArtifactDigest.ofFile(dependency).equals(pinned)) {
                throw new NativeProviderException(dependency.getFileName() + " is not the pinned build of that library");
            }
        } catch (IOException unreadable) {
            throw new UncheckedIOException(dependency.getFileName() + " could not be read", unreadable);
        }
    }
}
