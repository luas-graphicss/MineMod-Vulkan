package io.github.luasgraphicss.minemod.adapters.minecraft.mixin.window;

import com.mojang.blaze3d.platform.Window;
import io.github.luasgraphicss.minemod.adapters.minecraft.bootstrap.ClientRendering;
import io.github.luasgraphicss.minemod.adapters.minecraft.compatibility.ModRendering;
import io.github.luasgraphicss.minemod.bridge.PresentPacing;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GLCapabilities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
abstract class WindowMixin {
    @Redirect(
            method = "<init>",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwWindowHint(II)V", remap = false))
    private void minemod$openWithoutAnOpenGlContext(int hint, int value) {
        GLFW.glfwWindowHint(hint, hint == GLFW.GLFW_CLIENT_API ? GLFW.GLFW_NO_API : value);
    }

    @Redirect(
            method = "<init>",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwMakeContextCurrent(J)V", remap = false))
    private void minemod$presentThroughTheNativeRenderer(long window) {
        ClientRendering.attach(window);
    }

    @Redirect(
            method = "<init>",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lorg/lwjgl/opengl/GL;createCapabilities()Lorg/lwjgl/opengl/GLCapabilities;",
                            remap = false))
    private GLCapabilities minemod$resolveOpenGlThroughTheProviderOfMods() {
        return ModRendering.capabilities();
    }

    @Redirect(
            method = "updateVsync",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwSwapInterval(I)V", remap = false))
    private void minemod$paceThePresentation(int interval) {
        ClientRendering.renderer().pace(interval == 0 ? PresentPacing.IMMEDIATE : PresentPacing.VSYNC);
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void minemod$stopPresentingBeforeTheWindowCloses(CallbackInfo callback) {
        ClientRendering.detach();
    }
}
