package io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.mixin.opengl;

import com.mojang.blaze3d.platform.GlDebug;
import io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.bootstrap.ClientRendering;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlDebug.class)
abstract class GlDebugMixin {
    @Inject(method = "enableDebugCallback", at = @At("HEAD"), cancellable = true)
    private static void minemod$listenToNoOpenGlMessages(CallbackInfo callback) {
        if (ClientRendering.replacesOpenGl()) {
            callback.cancel();
        }
    }
}
