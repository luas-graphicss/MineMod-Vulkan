package io.github.luasgraphicss.minemod.client.textures;

import io.github.luasgraphicss.minemod.client.frame.StreamBudget;
import io.github.luasgraphicss.minemod.codec.scene.SceneCommandWriter;
import io.github.luasgraphicss.minemod.codec.scene.SceneOpcode;
import io.github.luasgraphicss.minemod.contract.handle.ResourceHandle;
import java.nio.ByteBuffer;

final class WaitingTexels {
    private static final long WRITE_BYTES = SceneOpcode.WRITE_TEXTURE.fixedBytes();

    private final TexelArea area;
    private final ByteBuffer texels;
    private int writtenRows;
    private boolean isReplaced;

    WaitingTexels(TexelArea area, ByteBuffer texels) {
        this.area = area;
        this.texels = texels.slice();
    }

    TexelArea area() {
        return area;
    }

    boolean isUntouched() {
        return writtenRows == 0;
    }

    void replace() {
        isReplaced = true;
    }

    boolean isWritten() {
        return isReplaced || writtenRows == area.height();
    }

    void writeRowsThatFit(ResourceHandle texture, SceneCommandWriter scene, StreamBudget budget) {
        if (isWritten()) {
            return;
        }
        long rowBytes = area.rowBytes();
        long fitting = Math.min(area.height() - writtenRows, (budget.room() - WRITE_BYTES) / rowBytes);
        if (fitting < 1 || !budget.take(WRITE_BYTES + fitting * rowBytes)) {
            return;
        }
        int rows = (int) fitting;
        ByteBuffer written = texels.duplicate();
        written.position((int) (writtenRows * rowBytes));
        written.limit((int) ((writtenRows + rows) * rowBytes));
        scene.writeTexture(texture, area.rows(writtenRows, rows).toRegion(), written);
        writtenRows += rows;
    }
}
