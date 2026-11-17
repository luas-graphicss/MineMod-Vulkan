package io.github.luasgraphicss.minemod.adapters.minecraft.shaders;

import io.github.luasgraphicss.minemod.client.shaders.ShaderFile;
import io.github.luasgraphicss.minemod.client.shaders.ShaderFiles;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import net.minecraft.ResourceLocationException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

public final class PackShaderFiles implements ShaderFiles {
    private static final String BUILT_IN_PACK = "Default";
    private static final int MAX_FILE_BYTES = 1 << 18;

    private final ResourceManager resources;

    public PackShaderFiles(ResourceManager resources) {
        this.resources = resources;
    }

    public static boolean replacesBuiltIn(Resource resource) {
        return !BUILT_IN_PACK.equals(resource.getSourceName());
    }

    @Override
    public Optional<ShaderFile> open(String path) {
        return locationOf(path).flatMap(this::read);
    }

    private static Optional<ResourceLocation> locationOf(String path) {
        try {
            return Optional.of(new ResourceLocation(path));
        } catch (ResourceLocationException malformed) {
            return Optional.empty();
        }
    }

    private Optional<ShaderFile> read(ResourceLocation location) {
        try (Resource resource = resources.getResource(location);
                InputStream stream = resource.getInputStream()) {
            byte[] bytes = stream.readNBytes(MAX_FILE_BYTES + 1);
            return Optional.of(new ShaderFile(new String(bytes, StandardCharsets.UTF_8), replacesBuiltIn(resource)));
        } catch (IOException unreadable) {
            return Optional.empty();
        }
    }
}
