package io.github.luasgraphicss.minemod.client.draw.frame;

import io.github.luasgraphicss.minemod.codec.scene.FrameBlur;
import io.github.luasgraphicss.minemod.codec.scene.SceneCommandWriter;
import io.github.luasgraphicss.minemod.codec.scene.SceneOpcode;

final class BlurredBackdrop implements FrameMark {
    private final int drawsBefore;
    private final FrameBlur blur;

    BlurredBackdrop(int drawsBefore, FrameBlur blur) {
        this.drawsBefore = drawsBefore;
        this.blur = blur;
    }

    @Override
    public int drawsBefore() {
        return drawsBefore;
    }

    @Override
    public long streamBytes() {
        return SceneOpcode.BLUR_FRAME.fixedBytes();
    }

    @Override
    public void write(SceneCommandWriter frame) {
        frame.blurFrame(blur);
    }
}
