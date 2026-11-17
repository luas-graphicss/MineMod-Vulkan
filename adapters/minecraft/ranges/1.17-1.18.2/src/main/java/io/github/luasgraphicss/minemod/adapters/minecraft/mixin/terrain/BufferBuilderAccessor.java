package io.github.luasgraphicss.minemod.adapters.minecraft.mixin.terrain;

import com.mojang.blaze3d.vertex.BufferBuilder;
import java.nio.ByteBuffer;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BufferBuilder.class)
public interface BufferBuilderAccessor {
    @Accessor("buffer")
    ByteBuffer minemod$buffer();

    @Accessor("drawStates")
    List<BufferBuilder.DrawState> minemod$drawStates();

    @Accessor("lastPoppedStateIndex")
    int minemod$lastPoppedStateIndex();

    @Accessor("totalUploadedBytes")
    int minemod$totalUploadedBytes();
}
