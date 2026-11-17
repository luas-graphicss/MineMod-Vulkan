package io.github.luasgraphicss.minemod.adapters.minecraft.mixin.reload;

import io.github.luasgraphicss.minemod.adapters.minecraft.reload.CoreShaderReloadListener;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.SimpleReloadableResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SimpleReloadableResourceManager.class)
abstract class ReloadableResourceManagerMixin {
    @Shadow
    public abstract void registerReloadListener(PreparableReloadListener listener);

    @Inject(method = "<init>", at = @At("RETURN"))
    private void minemod$reloadTheShadersOfTheNativeRenderer(PackType type, CallbackInfo callback) {
        if (type == PackType.CLIENT_RESOURCES) {
            registerReloadListener(new CoreShaderReloadListener());
        }
    }
}
