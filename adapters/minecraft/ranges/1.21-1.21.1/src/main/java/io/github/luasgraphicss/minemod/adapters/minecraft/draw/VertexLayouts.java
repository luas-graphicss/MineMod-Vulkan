package io.github.luasgraphicss.minemod.adapters.minecraft.draw;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import io.github.luasgraphicss.minemod.client.draw.vertex.GameVertexLayout;
import io.github.luasgraphicss.minemod.codec.scene.VertexElement;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

final class VertexLayouts {
    private static final Map<VertexFormat, Optional<GameVertexLayout>> LAYOUTS = new ConcurrentHashMap<>();

    private VertexLayouts() {}

    static Optional<GameVertexLayout> of(VertexFormat format) {
        return LAYOUTS.computeIfAbsent(format, VertexLayouts::describe);
    }

    private static Optional<GameVertexLayout> describe(VertexFormat format) {
        GameVertexLayout.Builder layout = GameVertexLayout.builder(format.getVertexSize());
        try {
            place(layout, format, VertexFormatElement.POSITION, VertexElement.POSITION);
            place(layout, format, VertexFormatElement.COLOR, VertexElement.COLOR);
            place(layout, format, VertexFormatElement.UV0, VertexElement.TEXTURE);
            place(layout, format, VertexFormatElement.UV1, VertexElement.OVERLAY);
            place(layout, format, VertexFormatElement.UV2, VertexElement.LIGHT);
            place(layout, format, VertexFormatElement.NORMAL, VertexElement.NORMAL);
            return Optional.of(layout.build());
        } catch (IllegalArgumentException unfit) {
            return Optional.empty();
        }
    }

    private static void place(
            GameVertexLayout.Builder layout, VertexFormat format, VertexFormatElement stored, VertexElement read) {
        if (format.contains(stored)) {
            layout.element(read, format.getOffset(stored));
        }
    }
}
