package io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.shaders;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import io.github.luasgraphicss.minemod.client.shaders.ProgramStages;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import org.slf4j.Logger;

public final class PackPrograms {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String CORE_DIRECTORY = "shaders/core";
    private static final String DEFINITION_EXTENSION = ".json";
    private static final String VERTEX_STAGE = "vertex";
    private static final String FRAGMENT_STAGE = "fragment";

    private PackPrograms() {}

    public static List<ProgramStages> definedIn(ResourceManager resources) {
        Map<ResourceLocation, List<Resource>> definitions =
                resources.listResourceStacks(CORE_DIRECTORY, PackPrograms::isDefinition);
        List<ProgramStages> programs = new ArrayList<>();
        definitions.forEach((location, providers) -> {
            if (providers.stream().anyMatch(provider -> !PackShaderFiles.replacesBuiltIn(provider))) {
                resources.getResource(location).flatMap(used -> stagesOf(location, used)).ifPresent(programs::add);
            }
        });
        return programs;
    }

    private static boolean isDefinition(ResourceLocation location) {
        return ResourceLocation.DEFAULT_NAMESPACE.equals(location.getNamespace())
                && location.getPath().endsWith(DEFINITION_EXTENSION);
    }

    private static Optional<ProgramStages> stagesOf(ResourceLocation location, Resource definition) {
        String path = location.getPath();
        String name = path.substring(CORE_DIRECTORY.length() + 1, path.length() - DEFINITION_EXTENSION.length());
        try (Reader reader = definition.openAsReader()) {
            JsonObject json = GsonHelper.parse(reader);
            return Optional.of(new ProgramStages(
                    name,
                    GsonHelper.getAsString(json, VERTEX_STAGE),
                    GsonHelper.getAsString(json, FRAGMENT_STAGE),
                    PackShaderFiles.replacesBuiltIn(definition)));
        } catch (IOException | JsonParseException malformed) {
            LOGGER.warn("Leaving the core shader {} to the native renderer: {}", name, malformed.getMessage());
            return Optional.empty();
        }
    }
}
