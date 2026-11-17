package io.github.luasgraphicss.minemod.adapters.minecraft.mixin.opengl;

import com.mojang.blaze3d.systems.TimerQuery;
import io.github.luasgraphicss.minemod.adapters.minecraft.bootstrap.ClientRendering;
import java.util.Optional;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TimerQuery.class)
abstract class TimerQueryMixin {
    @Inject(method = "getInstance", at = @At("HEAD"), cancellable = true)
    private static void minemod$timeNoOpenGlQueries(CallbackInfoReturnable<Optional<TimerQuery>> callback) {
        if (ClientRendering.replacesOpenGl()) {
            callback.setReturnValue(Optional.empty());
        }
    }
}
