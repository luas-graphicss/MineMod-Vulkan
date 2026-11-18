package io.github.luasgraphicss.minemod.adapters.minecraft.mixin.opengl;

import com.mojang.blaze3d.platform.GlStateManager;
import io.github.luasgraphicss.minemod.adapters.minecraft.bootstrap.ClientRendering;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlStateManager.class)
abstract class ShaderSourceCallsMixin {
    @Inject(method = "glShaderSource", at = @At("HEAD"), cancellable = true)
    private static void minemod$keepTheSource(int shader, List<String> source, CallbackInfo callback) {
        if (ClientRendering.replacesOpenGl()) {
            ClientRendering.openGl().programs().source(shader, source);
            callback.cancel();
        }
    }
}
