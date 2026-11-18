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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GlStateManager.class)
abstract class MatrixCallsMixin {
    @Inject(method = "matrixMode", at = @At("HEAD"), cancellable = true)
    private static void minemod$selectAStack(int mode, CallbackInfo callback) {
        if (ClientRendering.replacesOpenGl()) {
            FixedPipeline.current().matrices().select(mode);
            callback.cancel();
        }
    }

    @Inject(method = "loadIdentity", at = @At("HEAD"), cancellable = true)
    private static void minemod$startOver(CallbackInfo callback) {
        minemod$change(callback, stack -> stack.load(Matrix.identity()));
    }

    @Inject(method = "pushMatrix", at = @At("HEAD"), cancellable = true)
    private static void minemod$save(CallbackInfo callback) {
        minemod$change(callback, MatrixStack::push);
    }

    @Inject(method = "popMatrix", at = @At("HEAD"), cancellable = true)
    private static void minemod$restore(CallbackInfo callback) {
        minemod$change(callback, MatrixStack::pop);
    }

    @Inject(method = "getMatrix", at = @At("HEAD"), cancellable = true)
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

    @Inject(method = "getMatrix4f", at = @At("HEAD"), cancellable = true)
    private static void minemod$readAsAMatrix(int name, CallbackInfoReturnable<Matrix4f> callback) {
        if (!ClientRendering.replacesOpenGl()) {
            return;
        }
        Matrix4f read = new Matrix4f();
        FixedPipeline.current().matrices().read(name).ifPresent(matrix -> read.load(FloatBuffer.wrap(matrix)));
        callback.setReturnValue(read);
    }

    @Inject(method = "ortho", at = @At("HEAD"), cancellable = true)
    private static void minemod$flatten(
            double left, double right, double bottom, double top, double near, double far, CallbackInfo callback) {
        minemod$change(callback, stack -> stack.multiply(Matrix.orthographic(left, right, bottom, top, near, far)));
    }

    @Inject(method = "rotatef", at = @At("HEAD"), cancellable = true)
    private static void minemod$rotate(float degrees, float x, float y, float z, CallbackInfo callback) {
        minemod$change(callback, stack -> stack.multiply(Matrix.rotation(degrees, x, y, z)));
    }

    @Inject(method = "rotated", at = @At("HEAD"), cancellable = true)
    private static void minemod$rotatePrecisely(double degrees, double x, double y, double z, CallbackInfo callback) {
        minemod$change(
                callback, stack -> stack.multiply(Matrix.rotation((float) degrees, (float) x, (float) y, (float) z)));
    }

    @Inject(method = "scalef", at = @At("HEAD"), cancellable = true)
    private static void minemod$scale(float x, float y, float z, CallbackInfo callback) {
        minemod$change(callback, stack -> stack.multiply(Matrix.scale(x, y, z)));
    }

    @Inject(method = "scaled", at = @At("HEAD"), cancellable = true)
    private static void minemod$scalePrecisely(double x, double y, double z, CallbackInfo callback) {
        minemod$change(callback, stack -> stack.multiply(Matrix.scale((float) x, (float) y, (float) z)));
    }

    @Inject(method = "translatef", at = @At("HEAD"), cancellable = true)
    private static void minemod$move(float x, float y, float z, CallbackInfo callback) {
        minemod$change(callback, stack -> stack.multiply(Matrix.translation(x, y, z)));
    }

    @Inject(method = "translated", at = @At("HEAD"), cancellable = true)
    private static void minemod$movePrecisely(double x, double y, double z, CallbackInfo callback) {
        minemod$change(callback, stack -> stack.multiply(Matrix.translation((float) x, (float) y, (float) z)));
    }

    @Inject(method = "multMatrix(Ljava/nio/FloatBuffer;)V", at = @At("HEAD"), cancellable = true)
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

    @Inject(method = "multMatrix(Lcom/mojang/math/Matrix4f;)V", at = @At("HEAD"), cancellable = true)
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
