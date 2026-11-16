package io.github.luasgraphicss.minemod.client.draw.state;

import io.github.luasgraphicss.minemod.codec.scene.DepthCompare;
import java.util.Optional;

public final class DepthTranslation {
    private static final int EQUAL = 0x0202;
    private static final int LESS_OR_EQUAL = 0x0203;
    private static final int GREATER = 0x0204;
    private static final int ALWAYS = 0x0207;

    private DepthTranslation() {}

    public static Optional<DepthCompare> of(int function) {
        switch (function) {
            case LESS_OR_EQUAL:
                return Optional.of(DepthCompare.LESS_OR_EQUAL);
            case EQUAL:
                return Optional.of(DepthCompare.EQUAL);
            case GREATER:
                return Optional.of(DepthCompare.GREATER);
            case ALWAYS:
                return Optional.of(DepthCompare.ALWAYS);
            default:
                return Optional.empty();
        }
    }
}
