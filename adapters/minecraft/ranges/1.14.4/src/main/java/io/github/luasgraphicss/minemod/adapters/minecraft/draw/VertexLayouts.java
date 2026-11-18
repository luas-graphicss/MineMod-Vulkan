package io.github.luasgraphicss.minemod.adapters.minecraft.draw;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
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

    static boolean stores(VertexFormat format, VertexFormatElement element) {
        return format.getElements().contains(element);
    }

    private static Optional<GameVertexLayout> describe(VertexFormat format) {
        GameVertexLayout.Builder layout = GameVertexLayout.builder(format.getVertexSize());
        try {
            place(layout, format, DefaultVertexFormat.ELEMENT_POSITION, VertexElement.POSITION);
            place(layout, format, DefaultVertexFormat.ELEMENT_COLOR, VertexElement.COLOR);
            place(layout, format, DefaultVertexFormat.ELEMENT_UV0, VertexElement.TEXTURE);
            place(layout, format, DefaultVertexFormat.ELEMENT_UV1, VertexElement.LIGHT);
            place(layout, format, DefaultVertexFormat.ELEMENT_NORMAL, VertexElement.NORMAL);
            return Optional.of(layout.build());
        } catch (IllegalArgumentException unfit) {
            return Optional.empty();
        }
    }

    private static void place(
            GameVertexLayout.Builder layout, VertexFormat format, VertexFormatElement stored, VertexElement read) {
        int offset = 0;
        for (VertexFormatElement element : format.getElements()) {
            if (element.equals(stored)) {
                layout.element(read, offset);
                return;
            }
            offset += element.getByteSize();
        }
    }
}
