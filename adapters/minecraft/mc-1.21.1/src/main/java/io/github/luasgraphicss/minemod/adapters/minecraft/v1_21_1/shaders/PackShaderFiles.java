package io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.shaders;

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
    private static final String BUILT_IN_PACK = "vanilla";
    private static final int MAX_FILE_BYTES = 1 << 18;

    private final ResourceManager resources;

    public PackShaderFiles(ResourceManager resources) {
        this.resources = resources;
    }

    public static boolean replacesBuiltIn(Resource resource) {
        return !BUILT_IN_PACK.equals(resource.sourcePackId());
    }

    @Override
    public Optional<ShaderFile> open(String path) {
        return locationOf(path).flatMap(resources::getResource).flatMap(PackShaderFiles::read);
    }

    private static Optional<ResourceLocation> locationOf(String path) {
        try {
            return Optional.of(ResourceLocation.withDefaultNamespace(path));
        } catch (ResourceLocationException malformed) {
            return Optional.empty();
        }
    }

    private static Optional<ShaderFile> read(Resource resource) {
        try (InputStream stream = resource.open()) {
            byte[] bytes = stream.readNBytes(MAX_FILE_BYTES + 1);
            return Optional.of(new ShaderFile(new String(bytes, StandardCharsets.UTF_8), replacesBuiltIn(resource)));
        } catch (IOException unreadable) {
            return Optional.empty();
        }
    }
}
