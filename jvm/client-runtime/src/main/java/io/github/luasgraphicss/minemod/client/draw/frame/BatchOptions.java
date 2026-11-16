package io.github.luasgraphicss.minemod.client.draw.frame;

import io.github.luasgraphicss.minemod.client.frame.StreamBudget;
import io.github.luasgraphicss.minemod.codec.scene.QuadOrder;
import io.github.luasgraphicss.minemod.codec.scene.SceneCommandWriter;
import io.github.luasgraphicss.minemod.codec.scene.SceneOpcode;
import java.util.Arrays;

final class BatchOptions {
    static final BatchOptions UNCHANGED =
            new BatchOptions(null, new float[] {1.0f, 0.0f, 0.0f}, new float[] {0.0f, 1.0f, 0.0f}, QuadOrder.SUBMITTED);

    private static final long CLIP_BYTES = SceneOpcode.SET_CLIP.fixedBytes();
    private static final long NO_CLIP_BYTES = SceneOpcode.CLEAR_CLIP.fixedBytes();
    private static final long TRANSFORM_BYTES = SceneOpcode.SET_TEXTURE_TRANSFORM.fixedBytes();
    private static final long ORDER_BYTES = SceneOpcode.SET_QUAD_ORDER.fixedBytes();

    private final int[] clip;
    private final float[] uRow;
    private final float[] vRow;
    private final QuadOrder order;

    BatchOptions(int[] clip, float[] uRow, float[] vRow, QuadOrder order) {
        this.clip = clip;
        this.uRow = uRow;
        this.vRow = vRow;
        this.order = order;
    }

    BatchOptions clippedTo(int left, int bottom, int width, int height) {
        return new BatchOptions(new int[] {left, bottom, width, height}, uRow, vRow, order);
    }

    BatchOptions transformedBy(float[] textureURow, float[] textureVRow) {
        return new BatchOptions(clip, textureURow.clone(), textureVRow.clone(), order);
    }

    BatchOptions orderedBy(QuadOrder quads) {
        return new BatchOptions(clip, uRow, vRow, quads);
    }

    boolean reserveChangesFrom(BatchOptions previous, StreamBudget budget) {
        if (changesClip(previous) && !budget.take(clip == null ? NO_CLIP_BYTES : CLIP_BYTES)) {
            return false;
        }
        if (changesTransform(previous) && !budget.take(TRANSFORM_BYTES)) {
            return false;
        }
        return order == previous.order || budget.take(ORDER_BYTES);
    }

    void writeChangesFrom(BatchOptions previous, SceneCommandWriter frame) {
        if (changesClip(previous) && clip == null) {
            frame.clearClip();
        } else if (changesClip(previous)) {
            frame.setClip(clip[0], clip[1], clip[2], clip[3]);
        }
        if (changesTransform(previous)) {
            frame.setTextureTransform(uRow, vRow);
        }
        if (order != previous.order) {
            frame.setQuadOrder(order);
        }
    }

    private boolean changesClip(BatchOptions previous) {
        return !Arrays.equals(clip, previous.clip);
    }

    private boolean changesTransform(BatchOptions previous) {
        return !Arrays.equals(uRow, previous.uRow) || !Arrays.equals(vRow, previous.vRow);
    }
}
