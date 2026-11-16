package io.github.luasgraphicss.minemod.adapters.fabric.v1_21_1;

import io.github.luasgraphicss.minemod.compatibility.contracts.report.OriginResolver;
import java.util.Optional;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;

final class ModOrigins implements OriginResolver {
    private static final String CLASS_FILE_SUFFIX = ".class";

    @Override
    public Optional<String> locationOf(String className) {
        String classFile = className.replace('.', '/') + CLASS_FILE_SUFFIX;
        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            if (mod.findPath(classFile).isPresent()) {
                ModMetadata metadata = mod.getMetadata();
                return Optional.of(metadata.getId() + " " + metadata.getVersion().getFriendlyString());
            }
        }
        return Optional.empty();
    }
}
