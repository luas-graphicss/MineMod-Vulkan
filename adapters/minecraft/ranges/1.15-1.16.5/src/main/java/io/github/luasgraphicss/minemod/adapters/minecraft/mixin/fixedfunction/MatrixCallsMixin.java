package io.github.luasgraphicss.minemod.adapters.minecraft.mixin.fixedfunction;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.math.Matrix4f;
import io.github.luasgraphicss.minemod.adapters.minecraft.bootstrap.ClientRendering;
import io.github.luasgraphicss.minemod.adapters.minecraft.draw.Matrices;
import io.github.luasgraphicss.minemod.adapters.minecraft.fixedfunction.FixedPipeline;
import io.github.luasgraphicss.minemod.adapters.minecraft.fixedfunction.Matrix;
import io.github.luasgraphicss.minemod.adapters.minecraft.fixedfunction.MatrixStack;
import java.nio.FloatBuffer;
import java.util.function.Consumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlStateManager.class)
abstract class MatrixCallsMixin {
    @Inject(method = "_matrixMode", at = @At("HEAD"), cancellable = true)
    private static void minemod$selectAStack(int mode, CallbackInfo callback) {
        if (ClientRendering.replacesOpenGl()) {
            FixedPipeline.current().matrices().select(mode);
            callback.cancel();
        }
    }

    @Inject(method = "_loadIdentity", at = @At("HEAD"), cancellable = true)
    private static void minemod$startOver(CallbackInfo callback) {
        minemod$change(callback, stack -> stack.load(Matrix.identity()));
    }

    @Inject(method = "_pushMatrix", at = @At("HEAD"), cancellable = true)
    private static void minemod$save(CallbackInfo callback) {
        minemod$change(callback, MatrixStack::push);
    }

    @Inject(method = "_popMatrix", at = @At("HEAD"), cancellable = true)
    private static void minemod$restore(CallbackInfo callback) {
        minemod$change(callback, MatrixStack::pop);
    }

    @Inject(method = "_getMatrix", at = @At("HEAD"), cancellable = true)
    private static void minemod$read(int name, FloatBuffer into, CallbackInfo callback) {
        if (!ClientRendering.replacesOpenGl()) {
            return;
        }
        FixedPipeline.current().matrices().read(name).ifPresent(matrix -> {
            for (int value = 0; value < Matrix.VALUES && value < into.remaining(); value++) {
                into.put(into.position() + value, matrix[value]);
            }
        });
        callback.cancel();
    }

    @Inject(method = "_ortho", at = @At("HEAD"), cancellable = true)
    private static void minemod$flatten(
            double left, double right, double bottom, double top, double near, double far, CallbackInfo callback) {
        minemod$change(callback, stack -> stack.multiply(Matrix.orthographic(left, right, bottom, top, near, far)));
    }

    @Inject(method = "_rotatef", at = @At("HEAD"), cancellable = true)
    private static void minemod$rotate(float degrees, float x, float y, float z, CallbackInfo callback) {
        minemod$change(callback, stack -> stack.multiply(Matrix.rotation(degrees, x, y, z)));
    }

    @Inject(method = "_scalef", at = @At("HEAD"), cancellable = true)
    private static void minemod$scale(float x, float y, float z, CallbackInfo callback) {
        minemod$change(callback, stack -> stack.multiply(Matrix.scale(x, y, z)));
    }

    @Inject(method = "_scaled", at = @At("HEAD"), cancellable = true)
    private static void minemod$scalePrecisely(double x, double y, double z, CallbackInfo callback) {
        minemod$change(callback, stack -> stack.multiply(Matrix.scale((float) x, (float) y, (float) z)));
    }

    @Inject(method = "_translatef", at = @At("HEAD"), cancellable = true)
    private static void minemod$move(float x, float y, float z, CallbackInfo callback) {
        minemod$change(callback, stack -> stack.multiply(Matrix.translation(x, y, z)));
    }

    @Inject(method = "_translated", at = @At("HEAD"), cancellable = true)
    private static void minemod$movePrecisely(double x, double y, double z, CallbackInfo callback) {
        minemod$change(callback, stack -> stack.multiply(Matrix.translation((float) x, (float) y, (float) z)));
    }

    @Inject(method = "_multMatrix(Ljava/nio/FloatBuffer;)V", at = @At("HEAD"), cancellable = true)
    private static void minemod$multiplyByValues(FloatBuffer values, CallbackInfo callback) {
        if (values.remaining() < Matrix.VALUES) {
            return;
        }
        float[] matrix = new float[Matrix.VALUES];
        for (int value = 0; value < Matrix.VALUES; value++) {
            matrix[value] = values.get(values.position() + value);
        }
        minemod$change(callback, stack -> stack.multiply(matrix));
    }

    @Inject(method = "_multMatrix(Lcom/mojang/math/Matrix4f;)V", at = @At("HEAD"), cancellable = true)
    private static void minemod$multiply(Matrix4f matrix, CallbackInfo callback) {
        minemod$change(callback, stack -> stack.multiply(Matrices.columnMajor(matrix)));
    }

    @Unique
    private static void minemod$change(CallbackInfo callback, Consumer<MatrixStack> change) {
        if (ClientRendering.replacesOpenGl()) {
            change.accept(FixedPipeline.current().matrices().selected());
            callback.cancel();
        }
    }
}
