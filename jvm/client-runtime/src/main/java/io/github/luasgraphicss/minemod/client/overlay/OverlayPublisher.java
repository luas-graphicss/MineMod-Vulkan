package io.github.luasgraphicss.minemod.client.overlay;

import io.github.luasgraphicss.minemod.client.frame.StreamBudget;
import io.github.luasgraphicss.minemod.codec.scene.SceneCommandWriter;
import io.github.luasgraphicss.minemod.codec.scene.SceneOpcode;
import io.github.luasgraphicss.minemod.codec.scene.TextureRegion;
import io.github.luasgraphicss.minemod.codec.scene.TextureSampling;
import io.github.luasgraphicss.minemod.contract.handle.ResourceHandle;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public final class OverlayPublisher {
    public static final int EDGE_TEXELS = 16;
    public static final int TEXEL_BYTES = EDGE_TEXELS * EDGE_TEXELS * 4;

    private static final int SINGLE_LEVEL = 1;
    private static final Set<TextureSampling> NEAREST_AND_CLAMPED =
            Collections.unmodifiableSet(EnumSet.noneOf(TextureSampling.class));
    private static final TextureRegion WHOLE_TEXTURE = new TextureRegion(0, 0, 0, EDGE_TEXELS, EDGE_TEXELS);
    private static final long DEFINITION_BYTES = SceneOpcode.DEFINE_TEXTURE.fixedBytes();
    private static final long WRITE_BYTES = SceneOpcode.WRITE_TEXTURE.fixedBytes() + TEXEL_BYTES;

    private final ResourceHandle texture;
    private final ByteBuffer texels = ByteBuffer.allocate(TEXEL_BYTES);
    private boolean isDefined;
    private boolean hasUnpublishedTexels;

    public OverlayPublisher(ResourceHandle texture) {
        this.texture = texture;
    }

    public ResourceHandle texture() {
        return texture;
    }

    public boolean isDefined() {
        return isDefined;
    }

    public void update(ByteBuffer rgbaTexels) {
        if (rgbaTexels.remaining() != TEXEL_BYTES) {
            throw new IllegalArgumentException(
                    "the overlay takes " + TEXEL_BYTES + " bytes of RGBA texels, found " + rgbaTexels.remaining());
        }
        texels.clear();
        texels.put(rgbaTexels.duplicate());
        texels.flip();
        hasUnpublishedTexels = true;
    }

    public void publish(SceneCommandWriter scene, StreamBudget budget) {
        if (!hasUnpublishedTexels) {
            return;
        }
        if (!isDefined && budget.take(DEFINITION_BYTES)) {
            scene.defineTexture(texture, EDGE_TEXELS, EDGE_TEXELS, SINGLE_LEVEL, NEAREST_AND_CLAMPED);
            isDefined = true;
        }
        if (isDefined && budget.take(WRITE_BYTES)) {
            scene.writeTexture(texture, WHOLE_TEXTURE, texels);
            hasUnpublishedTexels = false;
        }
    }
}
