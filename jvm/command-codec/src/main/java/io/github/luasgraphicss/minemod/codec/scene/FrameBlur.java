package io.github.luasgraphicss.minemod.codec.scene;

import java.nio.ByteBuffer;

public final class FrameBlur {
    public static final float MAX_RADIUS = 64.0f;
    public static final int MAX_REPEATS = 8;

    private final float radius;
    private final int repeats;

    public FrameBlur(float radius, int repeats) {
        boolean isMeasured = radius >= 0.0f && radius <= MAX_RADIUS;
        if (!isMeasured || repeats < 1 || repeats > MAX_REPEATS) {
            throw new IllegalArgumentException("a blur takes a radius up to " + MAX_RADIUS + " and 1 to " + MAX_REPEATS
                    + " repeats: " + radius + ", " + repeats);
        }
        this.radius = radius;
        this.repeats = repeats;
    }

    public float radius() {
        return radius;
    }

    public int repeats() {
        return repeats;
    }

    void put(ByteBuffer payload) {
        payload.putFloat(radius).putInt(repeats);
    }
}
