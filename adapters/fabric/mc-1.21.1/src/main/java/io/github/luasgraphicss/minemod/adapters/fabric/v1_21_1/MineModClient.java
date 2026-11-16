package io.github.luasgraphicss.minemod.adapters.fabric.v1_21_1;

import io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.bootstrap.ClientRendering;
import io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.compatibility.ModRendering;
import io.github.luasgraphicss.minemod.client.compatibility.provider.ProviderManifest;
import io.github.luasgraphicss.minemod.client.library.BundledLibrary;
import io.github.luasgraphicss.minemod.client.library.NativePlatform;
import java.nio.file.Path;
import java.nio.file.Paths;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class MineModClient implements ClientModInitializer {
    private static final String NATIVE_CACHE = "minemod-vulkan/natives";
    private static final String PROVIDER_PROPERTY = "minemod.openGlProvider";
    private static final String SOFTWARE_PROVIDER_PROPERTY = "minemod.softwareOpenGl";

    @Override
    public void onInitializeClient() {
        Path cache = FabricLoader.getInstance().getGameDir().resolve(NATIVE_CACHE);
        ClassLoader bundle = MineModClient.class.getClassLoader();
        NativePlatform platform = NativePlatform.current();
        ClientRendering.prepare(new BundledLibrary(bundle, platform).extractInto(cache));
        String provider = System.getProperty(PROVIDER_PROPERTY);
        if (provider != null) {
            ModRendering.prepare(
                    new BundledLibrary(bundle, platform.openGlProvider()).extractInto(cache),
                    ProviderManifest.read(Paths.get(provider)),
                    new ModOrigins(),
                    Boolean.getBoolean(SOFTWARE_PROVIDER_PROPERTY));
        }
    }
}
