package io.github.luasgraphicss.minemod.adapters.minecraft.mixin.opengl;

import com.mojang.blaze3d.platform.GLX;
import io.github.luasgraphicss.minemod.adapters.minecraft.bootstrap.ClientRendering;
import io.github.luasgraphicss.minemod.adapters.minecraft.opengl.OpenGlAnswers;
import java.util.Collections;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GLX.class)
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

    @Inject(method = "glShaderSource", at = @At("HEAD"), cancellable = true)
    private static void minemod$keepTheSource(int shader, CharSequence source, CallbackInfo callback) {
        if (ClientRendering.replacesOpenGl()) {
            ClientRendering.openGl().programs().source(shader, Collections.singletonList(source.toString()));
            callback.cancel();
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

    @Inject(method = "glGetUniformLocation", at = @At("HEAD"), cancellable = true)
    private static void minemod$locateAUsedUniform(
            int program, CharSequence uniform, CallbackInfoReturnable<Integer> callback) {
        if (ClientRendering.replacesOpenGl()) {
            callback.setReturnValue(ClientRendering.openGl().programs().uniformLocation(program, uniform));
        }
    }

    @Inject(method = "glGetAttribLocation", at = @At("HEAD"), cancellable = true)
    private static void minemod$locateNoAttribute(CallbackInfoReturnable<Integer> callback) {
        if (ClientRendering.replacesOpenGl()) {
            callback.setReturnValue(OpenGlAnswers.FIRST_ATTRIBUTE);
        }
    }

    @Inject(
            method = {
                "glCompileShader",
                "glLinkProgram",
                "glUseProgram",
                "glUniform1*",
                "glUniform1i",
                "glUniform2*",
                "glUniform3*",
                "glUniform4*",
                "glUniformMatrix2",
                "glUniformMatrix3",
                "glUniformMatrix4"
            },
            at = @At("HEAD"),
            cancellable = true)
    private static void minemod$leaveProgramsToTheScene(CallbackInfo callback) {
        if (ClientRendering.replacesOpenGl()) {
            callback.cancel();
        }
    }
}
