package io.github.luasgraphicss.minemod.adapters.minecraft.shaders;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import io.github.luasgraphicss.minemod.client.shaders.ProgramStages;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class PackPrograms {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String CORE_DIRECTORY = "shaders/core";
    private static final String DEFINITION_EXTENSION = ".json";
    private static final String VERTEX_STAGE = "vertex";
    private static final String FRAGMENT_STAGE = "fragment";

    private PackPrograms() {}

    public static List<ProgramStages> definedIn(ResourceManager resources) {
        List<ProgramStages> programs = new ArrayList<>();
        for (ResourceLocation location : resources.listResources(CORE_DIRECTORY, PackPrograms::isDefinition)) {
            if (ResourceLocation.DEFAULT_NAMESPACE.equals(location.getNamespace())
                    && isProvidedByTheGame(resources, location)) {
                stagesOf(resources, location).ifPresent(programs::add);
            }
        }
        return programs;
    }

    private static boolean isDefinition(String fileName) {
        return fileName.endsWith(DEFINITION_EXTENSION);
    }

    private static boolean isProvidedByTheGame(ResourceManager resources, ResourceLocation location) {
        List<Resource> providers;
        try {
            providers = resources.getResources(location);
        } catch (IOException unlisted) {
            LOGGER.warn("Leaving the core shader {} to the native renderer: {}", location, unlisted.getMessage());
            return false;
        }
        boolean isProvidedByTheGame = false;
        for (Resource provider : providers) {
            isProvidedByTheGame |= !PackShaderFiles.replacesBuiltIn(provider);
            close(provider);
        }
        return isProvidedByTheGame;
    }

    private static void close(Resource provider) {
        try {
            provider.close();
        } catch (IOException unclosed) {
            LOGGER.warn("A provider of {} stayed open: {}", provider.getLocation(), unclosed.getMessage());
        }
    }

    private static Optional<ProgramStages> stagesOf(ResourceManager resources, ResourceLocation location) {
        String path = location.getPath();
        String name = path.substring(CORE_DIRECTORY.length() + 1, path.length() - DEFINITION_EXTENSION.length());
        try (Resource definition = resources.getResource(location);
                Reader reader = new InputStreamReader(definition.getInputStream(), StandardCharsets.UTF_8)) {
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
