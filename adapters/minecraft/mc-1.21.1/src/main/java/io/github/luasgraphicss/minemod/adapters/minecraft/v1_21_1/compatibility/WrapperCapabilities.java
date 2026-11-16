package io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.compatibility;

import io.github.luasgraphicss.minemod.compatibility.layer.state.TrackedCapability;
import io.github.luasgraphicss.minemod.compatibility.state.reconcile.LeftoverPolicy;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.opengl.GL11;

final class WrapperCapabilities {
    private static final int[] CACHED_BY_THE_GAME = {
        GL11.GL_BLEND,
        GL11.GL_DEPTH_TEST,
        GL11.GL_CULL_FACE,
        GL11.GL_POLYGON_OFFSET_FILL,
        GL11.GL_COLOR_LOGIC_OP,
        GL11.GL_SCISSOR_TEST,
    };

    private WrapperCapabilities() {}

    static List<TrackedCapability> tracked() {
        List<TrackedCapability> tracked = new ArrayList<>();
        for (int capability : CACHED_BY_THE_GAME) {
            tracked.add(new TrackedCapability(capability, LeftoverPolicy.RESTORE));
        }
        return tracked;
    }
}
