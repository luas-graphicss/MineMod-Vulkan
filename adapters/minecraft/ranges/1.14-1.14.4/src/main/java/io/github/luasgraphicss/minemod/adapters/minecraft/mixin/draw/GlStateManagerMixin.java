package io.github.luasgraphicss.minemod.adapters.minecraft.mixin.draw;

import com.mojang.blaze3d.platform.GlStateManager;
import io.github.luasgraphicss.minemod.adapters.minecraft.bootstrap.ClientRendering;
import io.github.luasgraphicss.minemod.adapters.minecraft.compatibility.ModRendering;
import io.github.luasgraphicss.minemod.adapters.minecraft.draw.CapturedState;
import io.github.luasgraphicss.minemod.adapters.minecraft.frame.GameFrame;
import io.github.luasgraphicss.minemod.client.compatibility.state.WrapperCall;
import io.github.luasgraphicss.minemod.client.compatibility.state.WrapperCalls;
import java.util.function.Consumer;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlStateManager.class)
abstract class GlStateManagerMixin {
    @Inject(method = "enableBlend", at = @At("HEAD"), cancellable = true)
    private static void minemod$blend(CallbackInfo callback) {
        minemod$keep(callback, GL11.GL_BLEND, true, state -> state.blend(true));
    }

    @Inject(method = "disableBlend", at = @At("HEAD"), cancellable = true)
    private static void minemod$stopBlending(CallbackInfo callback) {
        minemod$keep(callback, GL11.GL_BLEND, false, state -> state.blend(false));
    }

    @Inject(method = "blendFunc(II)V", at = @At("HEAD"), cancellable = true)
    private static void minemod$keepTheBlendFactors(int source, int destination, CallbackInfo callback) {
        ModRendering.left(WrapperCall.BLEND_FACTORS, source, destination, source, destination);
        minemod$keep(callback, state -> state.blendFactors(source, destination));
    }

    @Inject(method = "blendFuncSeparate(IIII)V", at = @At("HEAD"), cancellable = true)
    private static void minemod$keepTheColorBlendFactors(
            int sourceColor, int destinationColor, int sourceAlpha, int destinationAlpha, CallbackInfo callback) {
        ModRendering.left(WrapperCall.BLEND_FACTORS, sourceColor, destinationColor, sourceAlpha, destinationAlpha);
        minemod$keep(callback, state -> state.blendFactors(sourceColor, destinationColor));
    }

    @Inject(method = "enableDepthTest", at = @At("HEAD"), cancellable = true)
    private static void minemod$testDepth(CallbackInfo callback) {
        minemod$keep(callback, GL11.GL_DEPTH_TEST, true, state -> state.depthTest(true));
    }

    @Inject(method = "disableDepthTest", at = @At("HEAD"), cancellable = true)
    private static void minemod$stopTestingDepth(CallbackInfo callback) {
        minemod$keep(callback, GL11.GL_DEPTH_TEST, false, state -> state.depthTest(false));
    }

    @Inject(method = "depthFunc", at = @At("HEAD"), cancellable = true)
    private static void minemod$keepTheDepthFunction(int function, CallbackInfo callback) {
        ModRendering.left(WrapperCall.DEPTH_FUNCTION, function);
        minemod$keep(callback, state -> state.depthFunction(function));
    }

    @Inject(method = "depthMask", at = @At("HEAD"), cancellable = true)
    private static void minemod$keepTheDepthMask(boolean isWritten, CallbackInfo callback) {
        ModRendering.left(WrapperCall.DEPTH_MASK, WrapperCalls.word(isWritten));
        minemod$keep(callback, state -> state.depthWrite(isWritten));
    }

    @Inject(method = "colorMask", at = @At("HEAD"), cancellable = true)
    private static void minemod$keepTheColorMask(
            boolean red, boolean green, boolean blue, boolean alpha, CallbackInfo callback) {
        ModRendering.left(
                WrapperCall.COLOR_MASK,
                WrapperCalls.word(red),
                WrapperCalls.word(green),
                WrapperCalls.word(blue),
                WrapperCalls.word(alpha));
        minemod$keep(callback, state -> state.colorWrite(red || green || blue || alpha));
    }

    @Inject(method = "enableCull", at = @At("HEAD"), cancellable = true)
    private static void minemod$cullBackFaces(CallbackInfo callback) {
        minemod$keep(callback, GL11.GL_CULL_FACE, true, state -> state.cullBackFaces(true));
    }

    @Inject(method = "disableCull", at = @At("HEAD"), cancellable = true)
    private static void minemod$stopCulling(CallbackInfo callback) {
        minemod$keep(callback, GL11.GL_CULL_FACE, false, state -> state.cullBackFaces(false));
    }

    @Inject(method = "enablePolygonOffset", at = @At("HEAD"), cancellable = true)
    private static void minemod$offsetDepth(CallbackInfo callback) {
        minemod$keep(callback, GL11.GL_POLYGON_OFFSET_FILL, true, state -> state.depthOffset(true));
    }

    @Inject(method = "disablePolygonOffset", at = @At("HEAD"), cancellable = true)
    private static void minemod$stopOffsettingDepth(CallbackInfo callback) {
        minemod$keep(callback, GL11.GL_POLYGON_OFFSET_FILL, false, state -> state.depthOffset(false));
    }

    @Inject(method = "polygonOffset", at = @At("HEAD"), cancellable = true)
    private static void minemod$keepTheDepthOffset(float factor, float units, CallbackInfo callback) {
        ModRendering.left(WrapperCall.DEPTH_OFFSET, WrapperCalls.word(factor), WrapperCalls.word(units));
        minemod$keep(callback, state -> state.depthOffset(factor, units));
    }

    @Inject(method = "clearColor", at = @At("HEAD"), cancellable = true)
    private static void minemod$keepTheClearColor(
            float red, float green, float blue, float alpha, CallbackInfo callback) {
        minemod$keep(callback, state -> state.clearColor(red, green, blue, alpha));
    }

    @Inject(method = "clear", at = @At("HEAD"), cancellable = true)
    private static void minemod$clearTheFrame(int buffers, boolean checksError, CallbackInfo callback) {
        minemod$keep(callback, state -> GameFrame.cleared(buffers));
    }

    @Inject(method = "enableColorLogicOp", at = @At("HEAD"), cancellable = true)
    private static void minemod$combineColorsByLogic(CallbackInfo callback) {
        minemod$keep(callback, GL11.GL_COLOR_LOGIC_OP, true, state -> state.colorLogic(true));
    }

    @Inject(method = "disableColorLogicOp", at = @At("HEAD"), cancellable = true)
    private static void minemod$stopCombiningColorsByLogic(CallbackInfo callback) {
        minemod$keep(callback, GL11.GL_COLOR_LOGIC_OP, false, state -> state.colorLogic(false));
    }

    @Inject(method = "viewport", at = @At("HEAD"), cancellable = true)
    private static void minemod$keepTheViewportForMods(
            int left, int bottom, int width, int height, CallbackInfo callback) {
        ModRendering.left(WrapperCall.VIEWPORT, left, bottom, width, height);
        if (ClientRendering.replacesOpenGl()) {
            callback.cancel();
        }
    }

    @Inject(method = "logicOp(I)V", at = @At("HEAD"), cancellable = true)
    private static void minemod$keepTheLogicOperationForMods(int operation, CallbackInfo callback) {
        ModRendering.left(WrapperCall.LOGIC_OPERATION, operation);
        if (ClientRendering.replacesOpenGl()) {
            callback.cancel();
        }
    }

    @Inject(
            method = {
                "cullFace(I)V",
                "polygonMode",
                "blendEquation",
                "stencilFunc",
                "stencilMask",
                "stencilOp",
                "clearDepth",
                "clearStencil"
            },
            at = @At("HEAD"),
            cancellable = true)
    private static void minemod$leaveTheRestToTheScene(CallbackInfo callback) {
        if (ClientRendering.replacesOpenGl()) {
            callback.cancel();
        }
    }

    @Unique
    private static void minemod$keep(
            CallbackInfo callback, int capability, boolean isEnabled, Consumer<CapturedState> change) {
        if (ClientRendering.replacesOpenGl()) {
            ModRendering.capability(capability, isEnabled);
        }
        minemod$keep(callback, change);
    }

    @Unique
    private static void minemod$keep(CallbackInfo callback, Consumer<CapturedState> change) {
        if (ClientRendering.replacesOpenGl()) {
            change.accept(GameFrame.state());
            callback.cancel();
        }
    }
}
