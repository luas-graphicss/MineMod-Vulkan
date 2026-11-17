package io.github.luasgraphicss.minemod.adapters.minecraft.mixin.presentation;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.luasgraphicss.minemod.adapters.minecraft.bootstrap.ClientRendering;
import io.github.luasgraphicss.minemod.adapters.minecraft.frame.GameFrame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RenderSystem.class)
abstract class RenderSystemMixin {
    @Redirect(
            method = "flipFrame",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwSwapBuffers(J)V", remap = false))
    private static void minemod$presentTheFrame(long window) {
        GameFrame.present(ClientRendering.renderer());
    }
}
