package io.github.luasgraphicss.minemod.adapters.fabric.v1_21_1;

import io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.bootstrap.ClientRendering;
import io.github.luasgraphicss.minemod.client.library.BundledLibrary;
import io.github.luasgraphicss.minemod.client.library.NativePlatform;
import java.nio.file.Path;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class MineModClient implements ClientModInitializer {
    private static final String NATIVE_CACHE = "minemod-vulkan/natives";

    @Override
    public void onInitializeClient() {
        Path cache = FabricLoader.getInstance().getGameDir().resolve(NATIVE_CACHE);
        BundledLibrary bundled = new BundledLibrary(MineModClient.class.getClassLoader(), NativePlatform.current());
        ClientRendering.prepare(bundled.extractInto(cache));
    }
}
