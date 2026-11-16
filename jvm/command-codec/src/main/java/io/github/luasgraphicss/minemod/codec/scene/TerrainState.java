package io.github.luasgraphicss.minemod.codec.scene;

import io.github.luasgraphicss.minemod.contract.handle.ResourceHandle;
import java.nio.ByteBuffer;

public final class TerrainState {
    private final float[] viewProjection;
    private final double cameraX;
    private final double cameraY;
    private final double cameraZ;
    private final Fog fog;
    private final long atlas;
    private final long lightmap;

    public TerrainState(
            float[] viewProjection,
            double cameraX,
            double cameraY,
            double cameraZ,
            Fog fog,
            ResourceHandle atlas,
            ResourceHandle lightmap) {
        this.viewProjection = SceneFields.matrix(viewProjection);
        this.cameraX = cameraX;
        this.cameraY = cameraY;
        this.cameraZ = cameraZ;
        this.fog = fog;
        this.atlas = SceneFields.texture(atlas);
        this.lightmap = SceneFields.texture(lightmap);
    }

    void put(ByteBuffer payload) {
        SceneFields.putFloats(payload, viewProjection);
        payload.putDouble(cameraX).putDouble(cameraY).putDouble(cameraZ);
        fog.putColorAndRange(payload);
        payload.putInt(fog.shape().code()).putInt(0).putLong(atlas).putLong(lightmap);
    }
}
