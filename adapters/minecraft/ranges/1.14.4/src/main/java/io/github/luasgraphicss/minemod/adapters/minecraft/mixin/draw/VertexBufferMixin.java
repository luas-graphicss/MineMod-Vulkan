package io.github.luasgraphicss.minemod.adapters.minecraft.mixin.draw;

import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.luasgraphicss.minemod.adapters.minecraft.draw.RetainedMesh;
import io.github.luasgraphicss.minemod.adapters.minecraft.frame.GameFrame;
import java.nio.ByteBuffer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VertexBuffer.class)
abstract class VertexBufferMixin {
    @Shadow
    @Final
    private VertexFormat format;

    @Unique
    private RetainedMesh minemod$retained;

    @Inject(method = "upload", at = @At("HEAD"))
    private void minemod$retainTheUploadedMesh(ByteBuffer contents, CallbackInfo callback) {
        if (GameFrame.isRecording()) {
            minemod$retained = RetainedMesh.uploadedAs(format, contents).orElse(null);
        }
    }

    @Inject(method = "draw", at = @At("HEAD"), cancellable = true)
    private void minemod$recordTheRetainedMesh(int mode, CallbackInfo callback) {
        if (!GameFrame.isRecording()) {
            return;
        }
        if (minemod$retained == null) {
            GameFrame.leaveOut();
        } else {
            minemod$retained.record(mode);
        }
        callback.cancel();
    }

    @Inject(method = "delete", at = @At("HEAD"))
    private void minemod$releaseTheRetainedMesh(CallbackInfo callback) {
        minemod$retained = null;
    }
}
