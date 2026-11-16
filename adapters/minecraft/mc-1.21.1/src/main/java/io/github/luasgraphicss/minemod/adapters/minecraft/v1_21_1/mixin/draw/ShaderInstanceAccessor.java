package io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.mixin.draw;

import java.util.List;
import net.minecraft.client.renderer.ShaderInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ShaderInstance.class)
public interface ShaderInstanceAccessor {
    @Accessor("samplerNames")
    List<String> minemod$samplerNames();
}
