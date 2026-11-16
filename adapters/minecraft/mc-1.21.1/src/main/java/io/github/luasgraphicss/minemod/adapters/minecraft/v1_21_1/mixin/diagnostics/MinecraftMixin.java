package io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.mixin.diagnostics;

import io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.diagnostics.CrashDetails;
import net.minecraft.CrashReport;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
abstract class MinecraftMixin {
    @Inject(method = "fillReport(Lnet/minecraft/CrashReport;)Lnet/minecraft/CrashReport;", at = @At("RETURN"))
    private void minemod$describeTheNativeRenderer(CrashReport crash, CallbackInfoReturnable<CrashReport> callback) {
        CrashDetails.describeIn(crash);
    }
}
