package io.github.luasgraphicss.minemod.client.draw.state;

import io.github.luasgraphicss.minemod.codec.scene.Shading;
import io.github.luasgraphicss.minemod.codec.scene.VertexElement;
import java.util.EnumSet;
import java.util.Set;

public final class ShadingSupport {
    private ShadingSupport() {}

    public static Set<Shading> of(Set<Shading> asked, Set<VertexElement> elements, boolean hasTexture) {
        Set<Shading> supported = EnumSet.noneOf(Shading.class);
        for (Shading step : asked) {
            if (isSupported(step, elements, hasTexture)) {
                supported.add(step);
            }
        }
        return supported;
    }

    private static boolean isSupported(Shading step, Set<VertexElement> elements, boolean hasTexture) {
        switch (step) {
            case TEXTURE:
                return hasTexture && elements.contains(VertexElement.TEXTURE);
            case LIGHTMAP:
                return elements.contains(VertexElement.LIGHT);
            case OVERLAY:
                return elements.contains(VertexElement.OVERLAY);
            case NORMAL:
            case WIDE_LINES:
                return elements.contains(VertexElement.NORMAL);
            default:
                return true;
        }
    }
}
