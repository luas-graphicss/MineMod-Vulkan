package io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.mixin.draw;

import com.mojang.blaze3d.platform.GlStateManager;
import io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.bootstrap.ClientRendering;
import io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.draw.CapturedState;
import io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.frame.GameFrame;
import java.util.function.Consumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlStateManager.class)
abstract class GlStateManagerMixin {
    @Inject(method = "_enableBlend", at = @At("HEAD"), cancellable = true)
    private static void minemod$blend(CallbackInfo callback) {
        minemod$keep(callback, state -> state.blend(true));
    }

    @Inject(method = "_disableBlend", at = @At("HEAD"), cancellable = true)
    private static void minemod$stopBlending(CallbackInfo callback) {
        minemod$keep(callback, state -> state.blend(false));
    }

    @Inject(method = "_blendFunc", at = @At("HEAD"), cancellable = true)
    private static void minemod$keepTheBlendFactors(int source, int destination, CallbackInfo callback) {
        minemod$keep(callback, state -> state.blendFactors(source, destination));
    }

    @Inject(method = "_blendFuncSeparate", at = @At("HEAD"), cancellable = true)
    private static void minemod$keepTheColorBlendFactors(
            int sourceColor, int destinationColor, int sourceAlpha, int destinationAlpha, CallbackInfo callback) {
        minemod$keep(callback, state -> state.blendFactors(sourceColor, destinationColor));
    }

    @Inject(method = "_enableDepthTest", at = @At("HEAD"), cancellable = true)
    private static void minemod$testDepth(CallbackInfo callback) {
        minemod$keep(callback, state -> state.depthTest(true));
    }

    @Inject(method = "_disableDepthTest", at = @At("HEAD"), cancellable = true)
    private static void minemod$stopTestingDepth(CallbackInfo callback) {
        minemod$keep(callback, state -> state.depthTest(false));
    }

    @Inject(method = "_depthFunc", at = @At("HEAD"), cancellable = true)
    private static void minemod$keepTheDepthFunction(int function, CallbackInfo callback) {
        minemod$keep(callback, state -> state.depthFunction(function));
    }

    @Inject(method = "_depthMask", at = @At("HEAD"), cancellable = true)
    private static void minemod$keepTheDepthMask(boolean isWritten, CallbackInfo callback) {
        minemod$keep(callback, state -> state.depthWrite(isWritten));
    }

    @Inject(method = "_colorMask", at = @At("HEAD"), cancellable = true)
    private static void minemod$keepTheColorMask(
            boolean red, boolean green, boolean blue, boolean alpha, CallbackInfo callback) {
        minemod$keep(callback, state -> state.colorWrite(red || green || blue || alpha));
    }

    @Inject(method = "_enableCull", at = @At("HEAD"), cancellable = true)
    private static void minemod$cullBackFaces(CallbackInfo callback) {
        minemod$keep(callback, state -> state.cullBackFaces(true));
    }

    @Inject(method = "_disableCull", at = @At("HEAD"), cancellable = true)
    private static void minemod$stopCulling(CallbackInfo callback) {
        minemod$keep(callback, state -> state.cullBackFaces(false));
    }

    @Inject(method = "_enablePolygonOffset", at = @At("HEAD"), cancellable = true)
    private static void minemod$offsetDepth(CallbackInfo callback) {
        minemod$keep(callback, state -> state.depthOffset(true));
    }

    @Inject(method = "_disablePolygonOffset", at = @At("HEAD"), cancellable = true)
    private static void minemod$stopOffsettingDepth(CallbackInfo callback) {
        minemod$keep(callback, state -> state.depthOffset(false));
    }

    @Inject(method = "_polygonOffset", at = @At("HEAD"), cancellable = true)
    private static void minemod$keepTheDepthOffset(float factor, float units, CallbackInfo callback) {
        minemod$keep(callback, state -> state.depthOffset(factor, units));
    }

    @Inject(method = "_enableScissorTest", at = @At("HEAD"), cancellable = true)
    private static void minemod$clip(CallbackInfo callback) {
        minemod$keep(callback, state -> state.clip(true));
    }

    @Inject(method = "_disableScissorTest", at = @At("HEAD"), cancellable = true)
    private static void minemod$stopClipping(CallbackInfo callback) {
        minemod$keep(callback, state -> state.clip(false));
    }

    @Inject(method = "_scissorBox", at = @At("HEAD"), cancellable = true)
    private static void minemod$keepTheClipBox(int left, int bottom, int width, int height, CallbackInfo callback) {
        minemod$keep(callback, state -> state.clipBox(left, bottom, width, height));
    }

    @Inject(method = "_clearColor", at = @At("HEAD"), cancellable = true)
    private static void minemod$keepTheClearColor(
            float red, float green, float blue, float alpha, CallbackInfo callback) {
        minemod$keep(callback, state -> state.clearColor(red, green, blue, alpha));
    }

    @Inject(method = "_clear", at = @At("HEAD"), cancellable = true)
    private static void minemod$clearTheFrame(int buffers, boolean checksError, CallbackInfo callback) {
        minemod$keep(callback, state -> GameFrame.cleared(buffers));
    }

    @Inject(method = "glBlendFuncSeparate", at = @At("HEAD"), cancellable = true)
    private static void minemod$keepTheSeparateBlendFactors(
            int sourceColor, int destinationColor, int sourceAlpha, int destinationAlpha, CallbackInfo callback) {
        minemod$keep(callback, state -> state.blendFactors(sourceColor, destinationColor));
    }

    @Inject(method = "_enableColorLogicOp", at = @At("HEAD"), cancellable = true)
    private static void minemod$combineColorsByLogic(CallbackInfo callback) {
        minemod$keep(callback, state -> state.colorLogic(true));
    }

    @Inject(method = "_disableColorLogicOp", at = @At("HEAD"), cancellable = true)
    private static void minemod$stopCombiningColorsByLogic(CallbackInfo callback) {
        minemod$keep(callback, state -> state.colorLogic(false));
    }

    @Inject(
            method = {
                "_logicOp",
                "_viewport",
                "_polygonMode",
                "_blendEquation",
                "_stencilFunc",
                "_stencilMask",
                "_stencilOp",
                "_clearDepth",
                "_clearStencil"
            },
            at = @At("HEAD"),
            cancellable = true)
    private static void minemod$leaveTheRestToTheScene(CallbackInfo callback) {
        if (ClientRendering.replacesOpenGl()) {
            callback.cancel();
        }
    }

    @Unique
    private static void minemod$keep(CallbackInfo callback, Consumer<CapturedState> change) {
        if (ClientRendering.replacesOpenGl()) {
            change.accept(GameFrame.state());
            callback.cancel();
        }
    }
}
