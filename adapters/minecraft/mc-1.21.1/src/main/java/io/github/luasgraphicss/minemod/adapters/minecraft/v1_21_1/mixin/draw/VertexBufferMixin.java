package io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.mixin.draw;

import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.draw.RetainedMesh;
import io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.frame.GameFrame;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
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
    private void minemod$retainTheUploadedMesh(MeshData mesh, CallbackInfo callback) {
        if (GameFrame.isRecording()) {
            minemod$retained = RetainedMesh.copyOf(mesh).orElse(null);
        }
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
