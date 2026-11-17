package io.github.luasgraphicss.minemod.client.opengl.textures;

import io.github.luasgraphicss.minemod.client.textures.TexelArea;
import io.github.luasgraphicss.minemod.client.textures.TextureShape;
import java.nio.ByteBuffer;

public interface TextureMirror {
    int generate();

    void define(int texture, TextureShape shape);

    void write(int texture, TexelArea area, ByteBuffer rgba);

    void setParameter(int texture, int parameter, int value);

    void delete(int texture);
}
