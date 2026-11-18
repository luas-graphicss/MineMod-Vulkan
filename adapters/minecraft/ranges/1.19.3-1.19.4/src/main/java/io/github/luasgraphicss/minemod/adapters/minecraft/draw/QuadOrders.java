package io.github.luasgraphicss.minemod.adapters.minecraft.draw;

import io.github.luasgraphicss.minemod.codec.scene.QuadOrder;

final class QuadOrders {
    private QuadOrders() {}

    static QuadOrder current() {
        return QuadOrder.FARTHEST_FIRST;
    }
}
