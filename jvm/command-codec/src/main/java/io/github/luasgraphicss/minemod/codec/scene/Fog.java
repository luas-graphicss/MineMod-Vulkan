package io.github.luasgraphicss.minemod.codec.scene;

import java.nio.ByteBuffer;

public final class Fog {
    private final float red;
    private final float green;
    private final float blue;
    private final float alpha;
    private final float start;
    private final float end;
    private final FogShape shape;

    public Fog(float red, float green, float blue, float alpha, float start, float end, FogShape shape) {
        if (!(end > start)) {
            throw new IllegalArgumentException("fog must end farther than it starts: " + start + " to " + end);
        }
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
        this.start = start;
        this.end = end;
        this.shape = shape;
    }

    FogShape shape() {
        return shape;
    }

    void putColorAndRange(ByteBuffer payload) {
        payload.putFloat(red).putFloat(green).putFloat(blue).putFloat(alpha).putFloat(start).putFloat(end);
    }
}
