package io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.mixin.draw;

import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderSystem.class)
public interface RenderSystemAccessor {
    @Accessor("shaderLightDirections")
    static Vector3f[] minemod$lightDirections() {
        throw new AssertionError("the accessor is replaced when the mixin is applied");
    }
}
