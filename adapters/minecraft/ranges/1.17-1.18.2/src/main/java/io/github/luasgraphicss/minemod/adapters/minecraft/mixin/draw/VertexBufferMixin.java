package io.github.luasgraphicss.minemod.adapters.minecraft.mixin.draw;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Matrix4f;
import io.github.luasgraphicss.minemod.adapters.minecraft.draw.RetainedMesh;
import io.github.luasgraphicss.minemod.adapters.minecraft.frame.GameFrame;
import java.nio.ByteBuffer;
import net.minecraft.client.renderer.ShaderInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VertexBuffer.class)
abstract class VertexBufferMixin {
    @Unique
    private RetainedMesh minemod$retained;

    @Redirect(
            method = "upload_",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lcom/mojang/blaze3d/vertex/BufferBuilder;popNextBuffer()Lcom/mojang/datafixers/util/Pair;"))
    private Pair<BufferBuilder.DrawState, ByteBuffer> minemod$retainTheUploadedMesh(BufferBuilder builder) {
        Pair<BufferBuilder.DrawState, ByteBuffer> uploaded = builder.popNextBuffer();
        if (GameFrame.isRecording()) {
            minemod$retained = RetainedMesh.copyOf(uploaded.getFirst(), uploaded.getSecond())
                    .orElse(null);
        }
        return uploaded;
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
