package io.github.luasgraphicss.minemod.adapters.minecraft.screenshot;

import net.minecraft.client.Minecraft;

final class WindowColorTexture {
    private WindowColorTexture() {}

    static int name() {
        return Minecraft.getInstance().getMainRenderTarget().getColorTextureId();
    }
}
