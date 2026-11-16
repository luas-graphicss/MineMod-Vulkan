package io.github.luasgraphicss.minemod.codec.scene;

import io.github.luasgraphicss.minemod.codec.CommandStreamWriter;
import io.github.luasgraphicss.minemod.contract.handle.ResourceHandle;
import java.nio.ByteBuffer;
import java.util.Set;

public final class SceneCommandWriter {
    private static final int DRAWS_WORLD = 1;

    private final CommandStreamWriter stream;

    public SceneCommandWriter(CommandStreamWriter stream) {
        this.stream = stream;
    }

    public SceneCommandWriter defineTexture(
            ResourceHandle texture, int width, int height, int mipLevels, Set<TextureSampling> sampling) {
        if (width < 1 || height < 1 || mipLevels < 1) {
            throw new IllegalArgumentException(
                    "a texture needs texels and a level: " + width + "x" + height + ", " + mipLevels + " level(s)");
        }
        ByteBuffer payload = begin(SceneOpcode.DEFINE_TEXTURE);
        payload.putLong(SceneFields.texture(texture)).putInt(width).putInt(height).putInt(mipLevels);
        payload.putInt(TextureSampling.maskOf(sampling));
        return this;
    }

    public SceneCommandWriter writeTexture(ResourceHandle texture, TextureRegion region, ByteBuffer texels) {
        if (texels.remaining() != region.texelBytes()) {
            throw new IllegalArgumentException(
                    "the region holds " + region.texelBytes() + " texel bytes, found " + texels.remaining());
        }
        ByteBuffer payload = begin(SceneOpcode.WRITE_TEXTURE, texels.remaining());
        payload.putLong(SceneFields.texture(texture));
        region.put(payload);
        payload.putInt(0).putLong(texels.remaining()).put(texels.duplicate());
        return this;
    }

    public SceneCommandWriter destroyTexture(ResourceHandle texture) {
        begin(SceneOpcode.DESTROY_TEXTURE).putLong(SceneFields.texture(texture));
        return this;
    }

    public SceneCommandWriter publishSection(int x, int y, int z, int world, SectionMesh mesh) {
        ByteBuffer payload = begin(SceneOpcode.PUBLISH_SECTION, mesh.blobBytes());
        payload.putInt(x).putInt(y).putInt(z).putInt(world);
        mesh.putCounts(payload);
        payload.putLong(mesh.blobBytes());
        mesh.putBlob(payload);
        return this;
    }

    public SceneCommandWriter unloadSection(int x, int y, int z) {
        begin(SceneOpcode.UNLOAD_SECTION).putInt(x).putInt(y).putInt(z).putInt(0);
        return this;
    }

    public SceneCommandWriter changeWorld() {
        begin(SceneOpcode.CHANGE_WORLD);
        return this;
    }

    public SceneCommandWriter beginInterfaceFrame(float red, float green, float blue, float alpha) {
        return beginFrame(red, green, blue, alpha, 0);
    }

    public SceneCommandWriter beginWorldFrame(float red, float green, float blue, float alpha) {
        return beginFrame(red, green, blue, alpha, DRAWS_WORLD);
    }

    public SceneCommandWriter setView(ViewState view) {
        view.put(begin(SceneOpcode.SET_VIEW));
        return this;
    }

    public SceneCommandWriter setTerrain(TerrainState terrain) {
        terrain.put(begin(SceneOpcode.SET_TERRAIN));
        return this;
    }

    public SceneCommandWriter drawBatch(BatchState batch, ByteBuffer vertices) {
        int vertexBytes = batch.vertexBytes();
        if (vertices.remaining() == 0 || vertices.remaining() % vertexBytes != 0) {
            throw new IllegalArgumentException(
                    "the batch takes " + vertexBytes + " bytes per vertex, found " + vertices.remaining());
        }
        ByteBuffer payload = begin(SceneOpcode.DRAW_BATCH, vertices.remaining());
        batch.put(payload);
        payload.putLong(vertices.remaining()).put(vertices.duplicate());
        return this;
    }

    public SceneCommandWriter endFrame() {
        begin(SceneOpcode.END_FRAME);
        return this;
    }

    public SceneCommandWriter setClip(int left, int bottom, int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("a clip cannot have a negative size: " + width + "x" + height);
        }
        begin(SceneOpcode.SET_CLIP).putInt(left).putInt(bottom).putInt(width).putInt(height);
        return this;
    }

    public SceneCommandWriter clearClip() {
        begin(SceneOpcode.CLEAR_CLIP);
        return this;
    }

    public SceneCommandWriter setTextureTransform(float[] uRow, float[] vRow) {
        float[] checkedURow = SceneFields.textureRow(uRow);
        float[] checkedVRow = SceneFields.textureRow(vRow);
        ByteBuffer payload = begin(SceneOpcode.SET_TEXTURE_TRANSFORM);
        SceneFields.putFloats(payload, checkedURow);
        SceneFields.putFloats(payload, checkedVRow);
        return this;
    }

    public SceneCommandWriter setQuadOrder(QuadOrder order) {
        begin(SceneOpcode.SET_QUAD_ORDER).putInt(order.code()).putInt(0);
        return this;
    }

    private SceneCommandWriter beginFrame(float red, float green, float blue, float alpha, int flags) {
        begin(SceneOpcode.BEGIN_FRAME).putFloat(red).putFloat(green).putFloat(blue).putFloat(alpha).putInt(flags).putInt(0);
        return this;
    }

    private ByteBuffer begin(SceneOpcode opcode) {
        return stream.appendRecord(opcode.code(), opcode.fixedBytes());
    }

    private ByteBuffer begin(SceneOpcode opcode, int blobBytes) {
        return stream.appendRecord(opcode.code(), opcode.fixedBytes() + blobBytes);
    }
}
