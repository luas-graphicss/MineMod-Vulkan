package io.github.luasgraphicss.minemod.client.draw.frame;

import io.github.luasgraphicss.minemod.client.draw.state.ShadingSupport;
import io.github.luasgraphicss.minemod.client.draw.vertex.VertexRepacker;
import io.github.luasgraphicss.minemod.client.frame.FrameContent;
import io.github.luasgraphicss.minemod.client.frame.StreamBudget;
import io.github.luasgraphicss.minemod.codec.scene.BatchSlot;
import io.github.luasgraphicss.minemod.codec.scene.BatchState;
import io.github.luasgraphicss.minemod.codec.scene.FrameBlur;
import io.github.luasgraphicss.minemod.codec.scene.SceneCommandWriter;
import io.github.luasgraphicss.minemod.codec.scene.SceneOpcode;
import io.github.luasgraphicss.minemod.codec.scene.TerrainState;
import io.github.luasgraphicss.minemod.codec.scene.VertexElement;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class DrawRecorder implements FrameContent {
    private static final int MAX_BATCH_VERTICES = 1 << 20;
    private static final int FIRST_ARENA_BYTES = 256 * 1024;
    private static final long FRAME_START_BYTES = SceneOpcode.BEGIN_FRAME.fixedBytes();
    private static final long FRAME_END_BYTES = SceneOpcode.END_FRAME.fixedBytes();
    private static final long TERRAIN_BYTES = SceneOpcode.SET_TERRAIN.fixedBytes();
    private static final long BATCH_BYTES = SceneOpcode.DRAW_BATCH.fixedBytes();
    private static final float[] OPAQUE_BLACK = {0.0f, 0.0f, 0.0f, 1.0f};

    private final long streamBytes;
    private final long streamRecords;
    private final FrameViews views = new FrameViews();
    private final List<RecordedDraw> draws = new ArrayList<>();
    private final List<FrameMark> marks = new ArrayList<>();
    private ByteBuffer vertices = ByteBuffer.allocateDirect(FIRST_ARENA_BYTES);
    private StreamBudget budget;
    private BatchOptions lastOptions;
    private BatchOptions writtenOptions;
    private BatchSlot slot;
    private TerrainState terrain;
    private boolean drawsWorld;
    private float[] clearColor;
    private boolean isDepthClear;
    private boolean hasWrittenStart;
    private boolean hasWrittenTerrain;
    private int writtenViews;
    private long droppedDraws;
    private long approximatedDraws;

    public DrawRecorder(long streamBytes, long streamRecords) {
        this.streamBytes = streamBytes;
        this.streamRecords = streamRecords;
        beginFrame();
    }

    public void beginFrame() {
        budget = new StreamBudget(streamBytes, streamRecords);
        if (!budget.take(FRAME_START_BYTES) || !budget.take(FRAME_END_BYTES)) {
            throw new IllegalArgumentException("a frame needs room for its first and last records: " + streamBytes
                    + " bytes, " + streamRecords + " records");
        }
        views.clear();
        draws.clear();
        marks.clear();
        vertices.clear();
        lastOptions = BatchOptions.UNCHANGED;
        writtenOptions = BatchOptions.UNCHANGED;
        slot = BatchSlot.INTERFACE;
        terrain = null;
        drawsWorld = false;
        clearColor = OPAQUE_BLACK;
        isDepthClear = true;
        hasWrittenStart = false;
        hasWrittenTerrain = false;
        writtenViews = 0;
    }

    public boolean isPartlyWritten() {
        return hasWrittenStart;
    }

    public void clearColor(float red, float green, float blue, float alpha) {
        clearColor = new float[] {red, green, blue, alpha};
    }

    public void clearDepth() {
        if (!isDepthClear && mark(new DepthClear(draws.size()))) {
            isDepthClear = true;
        }
    }

    public boolean blur(FrameBlur blur) {
        boolean isRecorded = mark(new BlurredBackdrop(draws.size(), blur));
        if (!isRecorded) {
            droppedDraws++;
        }
        return isRecorded;
    }

    public void enterWorld() {
        if (hasWrittenStart) {
            throw new IllegalStateException("a part of this frame was already written without the world");
        }
        drawsWorld = true;
        slot = BatchSlot.SKY;
    }

    public void terrain(TerrainState state) {
        if (!drawsWorld) {
            throw new IllegalStateException("terrain belongs to a frame that draws the world");
        }
        if (terrain == null && !budget.take(TERRAIN_BYTES)) {
            throw new IllegalStateException("the stream of a frame has no room for the terrain state");
        }
        terrain = state;
    }

    public void enter(BatchSlot stage) {
        if (stage != BatchSlot.INTERFACE && !drawsWorld) {
            throw new IllegalStateException(stage + " belongs to a frame that draws the world");
        }
        slot = stage;
    }

    public boolean draw(GameDraw draw) {
        if (draw.vertexCount() == 0) {
            return true;
        }
        boolean isRecorded = record(draw);
        if (!isRecorded) {
            droppedDraws++;
        }
        return isRecorded;
    }

    public long droppedDraws() {
        return droppedDraws;
    }

    public long approximatedDraws() {
        return approximatedDraws;
    }

    @Override
    public void writePart(SceneCommandWriter frame) {
        writeRecorded(frame, slot != BatchSlot.SKY);
        draws.clear();
        marks.clear();
        vertices.clear();
        isDepthClear = false;
        budget = new StreamBudget(streamBytes, streamRecords);
        boolean awaitsTerrain = terrain != null && !hasWrittenTerrain;
        if (!budget.take(FRAME_END_BYTES) || (awaitsTerrain && !budget.take(TERRAIN_BYTES))) {
            throw new IllegalStateException("the stream of a frame has no room for the records that end it");
        }
    }

    @Override
    public void write(SceneCommandWriter frame) {
        writeRecorded(frame, true);
        frame.endFrame();
    }

    private void writeRecorded(SceneCommandWriter frame, boolean hasReachedTheTerrain) {
        if (!hasWrittenStart) {
            writeStart(frame);
            hasWrittenStart = true;
        }
        writtenViews = views.writeFrom(writtenViews, frame);
        if (terrain != null && !hasWrittenTerrain && hasReachedTheTerrain) {
            frame.setTerrain(terrain);
            hasWrittenTerrain = true;
        }
        int writtenMarks = 0;
        for (int drawn = 0; drawn < draws.size(); drawn++) {
            RecordedDraw draw = draws.get(drawn);
            writtenMarks = writeMarks(writtenMarks, drawn, frame);
            draw.options().writeChangesFrom(writtenOptions, frame);
            writtenOptions = draw.options();
            frame.drawBatch(draw.state(), verticesOf(draw));
        }
        writeMarks(writtenMarks, draws.size(), frame);
    }

    private void writeStart(SceneCommandWriter frame) {
        if (drawsWorld) {
            frame.beginWorldFrame(clearColor[0], clearColor[1], clearColor[2], clearColor[3]);
        } else {
            frame.beginInterfaceFrame(clearColor[0], clearColor[1], clearColor[2], clearColor[3]);
        }
    }

    private boolean mark(FrameMark mark) {
        boolean hasRoom = budget.take(mark.streamBytes());
        if (hasRoom) {
            marks.add(mark);
        }
        return hasRoom;
    }

    private int writeMarks(int firstMark, int drawsBefore, SceneCommandWriter frame) {
        int mark = firstMark;
        while (mark < marks.size() && marks.get(mark).drawsBefore() <= drawsBefore) {
            marks.get(mark).write(frame);
            mark++;
        }
        return mark;
    }

    private boolean record(GameDraw draw) {
        long vertexBytes = (long) draw.vertexCount() * draw.layout().packedStride();
        if (draw.vertexCount() > MAX_BATCH_VERTICES || !draw.options().reserveChangesFrom(lastOptions, budget)) {
            return false;
        }
        int view = views.indexOf(draw.view(), budget);
        if (view == FrameViews.NO_VIEW || !budget.take(BATCH_BYTES + vertexBytes)) {
            return false;
        }
        makeRoomFor((int) vertexBytes);
        int vertexOffset = vertices.position();
        VertexRepacker.pack(draw.layout(), draw.vertices(), draw.vertexCount(), vertices);
        draws.add(new RecordedDraw(batchOf(draw, view), vertexOffset, (int) vertexBytes, draw.options()));
        lastOptions = draw.options();
        isDepthClear = false;
        if (!draw.state().isExact()) {
            approximatedDraws++;
        }
        return true;
    }

    private BatchState batchOf(GameDraw draw, int view) {
        Set<VertexElement> elements = draw.layout().elements();
        float[] tint = draw.colorModulator();
        BatchState.Builder batch = BatchState.builder(slot, view, elements, draw.mode())
                .shading(ShadingSupport.of(draw.shading(), elements, draw.texture() != null))
                .program(draw.program())
                .modelView(draw.modelView())
                .colorModulator(tint[0], tint[1], tint[2], tint[3])
                .alphaCutoff(draw.alphaCutoff())
                .lineWidth(draw.lineWidth());
        if (draw.texture() != null) {
            batch.texture(draw.texture());
        }
        draw.state().applyTo(batch);
        return batch.build();
    }

    private void makeRoomFor(int vertexBytes) {
        if (vertices.remaining() >= vertexBytes) {
            return;
        }
        long needed = (long) vertices.position() + vertexBytes;
        long grown = Math.max(needed, 2L * vertices.capacity());
        ByteBuffer larger = ByteBuffer.allocateDirect((int) Math.min(grown, Math.max(needed, streamBytes)));
        vertices.flip();
        larger.put(vertices);
        vertices = larger;
    }

    private ByteBuffer verticesOf(RecordedDraw draw) {
        ByteBuffer recorded = vertices.duplicate();
        recorded.limit(draw.vertexOffset() + draw.vertexBytes());
        recorded.position(draw.vertexOffset());
        return recorded;
    }
}
