package io.github.luasgraphicss.minemod.adapters.minecraft.mixin.opengl;

import com.mojang.blaze3d.platform.GlStateManager;
import io.github.luasgraphicss.minemod.adapters.minecraft.bootstrap.ClientRendering;
import io.github.luasgraphicss.minemod.adapters.minecraft.opengl.OpenGlAnswers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GlStateManager.class)
abstract class ProgramCallsMixin {
    @Inject(method = "glCreateShader", at = @At("HEAD"), cancellable = true)
    private static void minemod$nameAShader(CallbackInfoReturnable<Integer> callback) {
        if (ClientRendering.replacesOpenGl()) {
            callback.setReturnValue(ClientRendering.openGl().programs().createShader());
        }
    }

    @Inject(method = "glCreateProgram", at = @At("HEAD"), cancellable = true)
    private static void minemod$nameAProgram(CallbackInfoReturnable<Integer> callback) {
        if (ClientRendering.replacesOpenGl()) {
            callback.setReturnValue(ClientRendering.openGl().programs().createProgram());
        }
    }

    @Inject(method = "glAttachShader", at = @At("HEAD"), cancellable = true)
    private static void minemod$attachAShader(int program, int shader, CallbackInfo callback) {
        if (ClientRendering.replacesOpenGl()) {
            ClientRendering.openGl().programs().attach(program, shader);
            callback.cancel();
        }
    }

    @Inject(method = "glDeleteShader", at = @At("HEAD"), cancellable = true)
    private static void minemod$forgetAShader(int shader, CallbackInfo callback) {
        if (ClientRendering.replacesOpenGl()) {
            ClientRendering.openGl().programs().deleteShader(shader);
            callback.cancel();
        }
    }

    @Inject(method = "glDeleteProgram", at = @At("HEAD"), cancellable = true)
    private static void minemod$forgetAProgram(int program, CallbackInfo callback) {
        if (ClientRendering.replacesOpenGl()) {
            ClientRendering.openGl().programs().deleteProgram(program);
            callback.cancel();
        }
    }

    @Inject(method = {"glGetShaderi", "glGetProgrami"}, at = @At("HEAD"), cancellable = true)
    private static void minemod$reportSuccess(int object, int parameter, CallbackInfoReturnable<Integer> callback) {
        if (ClientRendering.replacesOpenGl()) {
            callback.setReturnValue(OpenGlAnswers.statusOf(parameter));
        }
    }

    @Inject(method = {"glGetShaderInfoLog", "glGetProgramInfoLog"}, at = @At("HEAD"), cancellable = true)
    private static void minemod$reportNoLog(CallbackInfoReturnable<String> callback) {
        if (ClientRendering.replacesOpenGl()) {
            callback.setReturnValue(OpenGlAnswers.NO_LOG);
        }
    }

    @Inject(method = "_glGetUniformLocation", at = @At("HEAD"), cancellable = true)
    private static void minemod$locateAUsedUniform(
            int program, CharSequence uniform, CallbackInfoReturnable<Integer> callback) {
        if (ClientRendering.replacesOpenGl()) {
            callback.setReturnValue(ClientRendering.openGl().programs().uniformLocation(program, uniform));
        }
    }

    @Inject(method = "_glGetAttribLocation", at = @At("HEAD"), cancellable = true)
    private static void minemod$locateNoAttribute(CallbackInfoReturnable<Integer> callback) {
        if (ClientRendering.replacesOpenGl()) {
            callback.setReturnValue(OpenGlAnswers.FIRST_ATTRIBUTE);
        }
    }

    @Inject(
            method = {
                "glCompileShader",
                "glLinkProgram",
                "_glUseProgram",
                "_glUniform1*",
                "_glUniform1i",
                "_glUniform2*",
                "_glUniform3*",
                "_glUniform4*",
                "_glUniformMatrix2",
                "_glUniformMatrix3",
                "_glUniformMatrix4"
            },
            at = @At("HEAD"),
            cancellable = true)
    private static void minemod$leaveProgramsToTheScene(CallbackInfo callback) {
        if (ClientRendering.replacesOpenGl()) {
            callback.cancel();
        }
    }
}
