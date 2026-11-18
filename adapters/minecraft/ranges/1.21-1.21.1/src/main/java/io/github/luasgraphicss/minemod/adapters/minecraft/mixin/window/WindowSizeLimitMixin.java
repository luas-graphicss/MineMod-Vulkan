package io.github.luasgraphicss.minemod.adapters.minecraft.mixin.window;

import com.mojang.blaze3d.platform.Window;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Window.class)
abstract class WindowSizeLimitMixin {
    @Redirect(
            method = "<init>",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lcom/mojang/blaze3d/systems/RenderSystem;maxSupportedTextureSize()I"))
    private int minemod$leaveTheWindowSizeToTheSwapchain() {
        return GLFW.GLFW_DONT_CARE;
    }
}
