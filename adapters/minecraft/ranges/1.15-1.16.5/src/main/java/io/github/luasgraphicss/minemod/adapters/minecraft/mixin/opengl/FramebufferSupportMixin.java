package io.github.luasgraphicss.minemod.adapters.minecraft.mixin.opengl;

import com.mojang.blaze3d.platform.GlStateManager;
import io.github.luasgraphicss.minemod.adapters.minecraft.bootstrap.ClientRendering;
import io.github.luasgraphicss.minemod.adapters.minecraft.opengl.FramebufferNames;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GlStateManager.class)
abstract class FramebufferSupportMixin {
    @Inject(method = "_init_fbo", at = @At("HEAD"), cancellable = true)
    private static void minemod$nameFramebuffersWithoutCapabilities(CallbackInfoReturnable<String> callback) {
        if (ClientRendering.replacesOpenGl()) {
            FramebufferNames.assign();
            callback.setReturnValue(FramebufferNames.SUPPORT);
        }
    }
}
