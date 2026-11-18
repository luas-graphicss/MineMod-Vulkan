package io.github.luasgraphicss.minemod.adapters.minecraft.mixin.frame;

import io.github.luasgraphicss.minemod.adapters.minecraft.bootstrap.ClientRendering;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
abstract class ShaderTransparencyMixin {
    @Inject(method = "useShaderTransparency", at = @At("HEAD"), cancellable = true)
    private static void minemod$blendTranslucencyIntoTheFrame(CallbackInfoReturnable<Boolean> callback) {
        if (ClientRendering.replacesOpenGl()) {
            callback.setReturnValue(false);
        }
    }
}
