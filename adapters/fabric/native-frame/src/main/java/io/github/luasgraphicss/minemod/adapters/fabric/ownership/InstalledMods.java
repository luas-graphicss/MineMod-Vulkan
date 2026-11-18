package io.github.luasgraphicss.minemod.adapters.fabric.ownership;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;

final class InstalledMods {
    private static final String PROVIDED_BY = " provided by ";

    private InstalledMods() {}

    static Map<String, String> versionsById(Collection<ModContainer> mods) {
        Map<String, String> versions = new TreeMap<>();
        for (ModContainer mod : mods) {
            ModMetadata metadata = mod.getMetadata();
            String version = metadata.getVersion().getFriendlyString();
            versions.put(metadata.getId(), version);
            for (String providedId : metadata.getProvides()) {
                versions.putIfAbsent(providedId, version + PROVIDED_BY + metadata.getId());
            }
        }
        return versions;
    }
}
