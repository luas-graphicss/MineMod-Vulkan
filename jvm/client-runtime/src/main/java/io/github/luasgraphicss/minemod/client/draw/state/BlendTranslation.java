package io.github.luasgraphicss.minemod.client.draw.state;

import io.github.luasgraphicss.minemod.codec.scene.Blending;
import java.util.Optional;

public final class BlendTranslation {
    private static final int ZERO = 0;
    private static final int ONE = 1;
    private static final int SOURCE_COLOR = 0x0300;
    private static final int ONE_MINUS_SOURCE_COLOR = 0x0301;
    private static final int SOURCE_ALPHA = 0x0302;
    private static final int ONE_MINUS_SOURCE_ALPHA = 0x0303;
    private static final int DESTINATION_COLOR = 0x0306;
    private static final int ONE_MINUS_DESTINATION_COLOR = 0x0307;

    private BlendTranslation() {}

    public static Optional<Blending> of(int sourceColor, int destinationColor) {
        if (sourceColor == SOURCE_ALPHA && destinationColor == ONE_MINUS_SOURCE_ALPHA) {
            return Optional.of(Blending.ALPHA);
        }
        if (sourceColor == ONE && destinationColor == ONE) {
            return Optional.of(Blending.ADDITIVE);
        }
        if (sourceColor == SOURCE_ALPHA && destinationColor == ONE) {
            return Optional.of(Blending.LIGHTNING);
        }
        if (sourceColor == SOURCE_COLOR && destinationColor == ONE) {
            return Optional.of(Blending.GLINT);
        }
        if (sourceColor == DESTINATION_COLOR && destinationColor == SOURCE_COLOR) {
            return Optional.of(Blending.CRUMBLING);
        }
        if (sourceColor == ONE_MINUS_DESTINATION_COLOR && destinationColor == ONE_MINUS_SOURCE_COLOR) {
            return Optional.of(Blending.INVERSION);
        }
        if (sourceColor == ZERO && destinationColor == ONE_MINUS_SOURCE_COLOR) {
            return Optional.of(Blending.DARKENING);
        }
        if (sourceColor == ONE && destinationColor == ZERO) {
            return Optional.of(Blending.DISABLED);
        }
        return Optional.empty();
    }
}
