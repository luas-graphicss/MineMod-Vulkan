package io.github.luasgraphicss.minemod.adapters.minecraft.mixin.draw;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.math.Matrix4f;
import io.github.luasgraphicss.minemod.adapters.minecraft.draw.RetainedMesh;
import io.github.luasgraphicss.minemod.adapters.minecraft.frame.GameFrame;
import net.minecraft.client.renderer.ShaderInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VertexBuffer.class)
abstract class VertexBufferMixin {
    @Unique
    private RetainedMesh minemod$retained;

    @Inject(method = "upload", at = @At("HEAD"))
    private void minemod$retainTheUploadedMesh(BufferBuilder.RenderedBuffer rendered, CallbackInfo callback) {
        if (!GameFrame.isRecording()) {
            return;
        }
        BufferBuilder.DrawState uploaded = rendered.drawState();
        minemod$retained = uploaded.indexOnly()
                ? null
                : RetainedMesh.copyOf(uploaded, rendered.vertexBuffer()).orElse(null);
    }

    @Inject(method = "drawWithShader", at = @At("HEAD"), cancellable = true)
    private void minemod$recordTheRetainedMesh(
            Matrix4f modelView, Matrix4f projection, ShaderInstance shader, CallbackInfo callback) {
        if (!GameFrame.isRecording()) {
            return;
        }
        if (minemod$retained == null) {
            GameFrame.leaveOut();
        } else {
            minemod$retained.record(modelView, projection, shader);
        }
        callback.cancel();
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void minemod$releaseTheRetainedMesh(CallbackInfo callback) {
        minemod$retained = null;
    }
}
