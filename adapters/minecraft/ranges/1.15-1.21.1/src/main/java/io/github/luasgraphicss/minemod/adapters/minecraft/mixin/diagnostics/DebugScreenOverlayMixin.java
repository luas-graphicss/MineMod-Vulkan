package io.github.luasgraphicss.minemod.adapters.minecraft.mixin.diagnostics;

import io.github.luasgraphicss.minemod.adapters.minecraft.diagnostics.DebugScreenLines;
import java.util.List;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DebugScreenOverlay.class)
abstract class DebugScreenOverlayMixin {
    @Inject(method = "getSystemInformation", at = @At("RETURN"))
    private void minemod$describeTheNativeRenderer(CallbackInfoReturnable<List<String>> callback) {
        DebugScreenLines.appendTo(callback.getReturnValue());
    }
}
