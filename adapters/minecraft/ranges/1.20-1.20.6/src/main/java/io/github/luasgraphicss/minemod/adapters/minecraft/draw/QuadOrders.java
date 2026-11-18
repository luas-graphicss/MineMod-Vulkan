package io.github.luasgraphicss.minemod.adapters.minecraft.draw;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import io.github.luasgraphicss.minemod.codec.scene.QuadOrder;

final class QuadOrders {
    private QuadOrders() {}

    static QuadOrder current() {
        return RenderSystem.getVertexSorting() == VertexSorting.ORTHOGRAPHIC_Z
                ? QuadOrder.RISING_DEPTH
                : QuadOrder.FARTHEST_FIRST;
    }
}
