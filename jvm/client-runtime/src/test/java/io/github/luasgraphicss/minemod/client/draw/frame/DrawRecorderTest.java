package io.github.luasgraphicss.minemod.client.draw.frame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.client.draw.state.RenderState;
import io.github.luasgraphicss.minemod.client.draw.vertex.GameVertexLayout;
import io.github.luasgraphicss.minemod.client.support.RecordedStream;
import io.github.luasgraphicss.minemod.codec.scene.BatchSlot;
import io.github.luasgraphicss.minemod.codec.scene.Fog;
import io.github.luasgraphicss.minemod.codec.scene.FogShape;
import io.github.luasgraphicss.minemod.codec.scene.FrameBlur;
import io.github.luasgraphicss.minemod.codec.scene.PrimitiveMode;
import io.github.luasgraphicss.minemod.codec.scene.QuadOrder;
import io.github.luasgraphicss.minemod.codec.scene.SceneOpcode;
import io.github.luasgraphicss.minemod.codec.scene.Shading;
import io.github.luasgraphicss.minemod.codec.scene.TerrainState;
import io.github.luasgraphicss.minemod.codec.scene.VertexElement;
import io.github.luasgraphicss.minemod.contract.handle.ResourceHandle;
import io.github.luasgraphicss.minemod.contract.handle.ResourceKind;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;

class DrawRecorderTest {
    private static final int SLOT_OFFSET = 0;
    private static final int VIEW_OFFSET = 4;
    private static final int ELEMENTS_OFFSET = 8;
    private static final int BLENDING_OFFSET = 16;
    private static final int DEPTH_OFFSET = 20;
    private static final int CULLING_OFFSET = 28;
    private static final int SHADING_OFFSET = 32;
    private static final int VERTICES_OFFSET = 152;
    private static final int SKY = 0;
    private static final int SOLID = 1;
    private static final int INTERFACE = 4;
    private static final int ALPHA_BLENDING = 1;
    private static final int TESTS_AND_WRITES_DEPTH = 3;
    private static final int CULLING_DISABLED = 1;
    private static final int POSITION_COLOR_TEXTURE = 7;
    private static final int TEXTURE_SHADING = 1;
    private static final int SOURCE_ALPHA = 0x0302;
    private static final int ONE_MINUS_SOURCE_ALPHA = 0x0303;
    private static final int DESTINATION_ALPHA = 0x0304;
    private static final int DEPTH_LESS_OR_EQUAL = 0x0203;
    private static final int GREEN = 0xFF00FF00;
    private static final float FIVE_PIXELS = 5.0f;
    private static final int THRICE = 3;
    private static final ResourceHandle ATLAS = ResourceHandle.of(ResourceKind.TEXTURE, 1, 0);
    private static final ResourceHandle LIGHTMAP = ResourceHandle.of(ResourceKind.TEXTURE, 2, 0);
    private static final float[] IDENTITY = {
        1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f,
    };
    private static final float[] HALVED = {
        0.5f, 0.0f, 0.0f, 0.0f, 0.0f, 0.5f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f,
    };
    private static final GameVertexLayout POSITION_COLOR = GameVertexLayout.builder(16)
            .element(VertexElement.POSITION, 0)
            .element(VertexElement.COLOR, 12)
            .build();
    private static final GameVertexLayout POSITION_TEXTURE_COLOR = GameVertexLayout.builder(24)
            .element(VertexElement.POSITION, 0)
            .element(VertexElement.TEXTURE, 12)
            .element(VertexElement.COLOR, 20)
            .build();

    private final DrawRecorder recorder = new DrawRecorder(32 * 1024, 64);

    @Test
    void aFrameWithoutAWorldHoldsItsViewAndItsBatchInTheInterface() {
        assertTrue(recorder.draw(triangle(IDENTITY).build()));

        RecordedStream stream = written(recorder);

        assertEquals(
                Arrays.asList(
                        SceneOpcode.BEGIN_FRAME, SceneOpcode.SET_VIEW, SceneOpcode.DRAW_BATCH, SceneOpcode.END_FRAME),
                stream.opcodes());
        assertEquals(INTERFACE, stream.payload(2).getInt(SLOT_OFFSET));
        assertEquals(GREEN, stream.payload(2).getInt(VERTICES_OFFSET + 12));
    }

    @Test
    void drawsSeenThroughTheSameViewDefineItOnce() {
        recorder.draw(triangle(IDENTITY).build());
        recorder.draw(triangle(IDENTITY).build());
        recorder.draw(triangle(HALVED).build());

        RecordedStream stream = written(recorder);

        assertEquals(
                Arrays.asList(
                        SceneOpcode.BEGIN_FRAME,
                        SceneOpcode.SET_VIEW,
                        SceneOpcode.SET_VIEW,
                        SceneOpcode.DRAW_BATCH,
                        SceneOpcode.DRAW_BATCH,
                        SceneOpcode.DRAW_BATCH,
                        SceneOpcode.END_FRAME),
                stream.opcodes());
        assertEquals(0, stream.payload(4).getInt(VIEW_OFFSET));
        assertEquals(1, stream.payload(5).getInt(VIEW_OFFSET));
    }

    @Test
    void optionsAreWrittenOnlyWhenTheyChange() {
        recorder.draw(triangle(IDENTITY).clip(0, 0, 8, 8).build());
        recorder.draw(triangle(IDENTITY).clip(0, 0, 8, 8).build());
        recorder.draw(triangle(IDENTITY).quadOrder(QuadOrder.FARTHEST_FIRST).build());

        RecordedStream stream = written(recorder);

        assertEquals(
                Arrays.asList(
                        SceneOpcode.BEGIN_FRAME,
                        SceneOpcode.SET_VIEW,
                        SceneOpcode.SET_CLIP,
                        SceneOpcode.DRAW_BATCH,
                        SceneOpcode.DRAW_BATCH,
                        SceneOpcode.CLEAR_CLIP,
                        SceneOpcode.SET_QUAD_ORDER,
                        SceneOpcode.DRAW_BATCH,
                        SceneOpcode.END_FRAME),
                stream.opcodes());
    }

    @Test
    void aTextureTransformIsWrittenBeforeTheBatchItMoves() {
        recorder.draw(triangle(IDENTITY)
                .textureTransform(new float[] {1.0f, 0.0f, 0.5f}, new float[] {0.0f, 1.0f, 0.25f})
                .build());

        RecordedStream stream = written(recorder);

        assertEquals(SceneOpcode.SET_TEXTURE_TRANSFORM, stream.opcodes().get(2));
        assertEquals(0.5f, stream.payload(2).getFloat(8));
        assertEquals(0.25f, stream.payload(2).getFloat(20));
    }

    @Test
    void aFrameOfTheWorldCarriesItsTerrainAndFillsTheSlotsItEnters() {
        Fog fog = new Fog(0.5f, 0.6f, 0.7f, 1.0f, 16.0f, 64.0f, FogShape.CYLINDER);
        recorder.enterWorld();
        recorder.terrain(new TerrainState(IDENTITY, 0.0, 64.0, 0.0, fog, ATLAS, LIGHTMAP));
        recorder.draw(triangle(IDENTITY).build());
        recorder.enter(BatchSlot.SOLID);
        recorder.draw(triangle(IDENTITY).build());
        recorder.enter(BatchSlot.INTERFACE);
        recorder.draw(triangle(IDENTITY).build());

        RecordedStream stream = written(recorder);

        assertEquals(
                Arrays.asList(
                        SceneOpcode.BEGIN_FRAME,
                        SceneOpcode.SET_VIEW,
                        SceneOpcode.SET_TERRAIN,
                        SceneOpcode.DRAW_BATCH,
                        SceneOpcode.DRAW_BATCH,
                        SceneOpcode.DRAW_BATCH,
                        SceneOpcode.END_FRAME),
                stream.opcodes());
        assertEquals(SKY, stream.payload(3).getInt(SLOT_OFFSET));
        assertEquals(SOLID, stream.payload(4).getInt(SLOT_OFFSET));
        assertEquals(INTERFACE, stream.payload(5).getInt(SLOT_OFFSET));
    }

    @Test
    void theWorldIsRefusedInAFrameThatDoesNotDrawIt() {
        Fog fog = new Fog(0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, FogShape.SPHERE);

        assertThrows(IllegalStateException.class, () -> recorder.enter(BatchSlot.SOLID));
        assertThrows(
                IllegalStateException.class,
                () -> recorder.terrain(new TerrainState(IDENTITY, 0.0, 0.0, 0.0, fog, ATLAS, LIGHTMAP)));
    }

    @Test
    void theStateOfTheGameReachesTheBatch() {
        RenderState state = RenderState.builder()
                .blend(SOURCE_ALPHA, ONE_MINUS_SOURCE_ALPHA)
                .depthTest(DEPTH_LESS_OR_EQUAL)
                .depthWrite(true)
                .build();
        ByteBuffer vertices = ByteBuffer.allocate(72).order(ByteOrder.LITTLE_ENDIAN);
        for (int corner = 0; corner < 3; corner++) {
            vertices.putFloat(corner).putFloat(0.0f).putFloat(0.0f).putFloat(0.5f).putFloat(1.0f).putInt(GREEN);
        }
        vertices.flip();
        recorder.draw(GameDraw.builder(POSITION_TEXTURE_COLOR, PrimitiveMode.TRIANGLES, vertices, 3)
                .view(GameView.builder(IDENTITY).build())
                .state(state)
                .shading(EnumSet.of(Shading.TEXTURE, Shading.LIGHTMAP))
                .texture(ATLAS)
                .build());

        ByteBuffer batch = batchOf(written(recorder), 2);

        assertEquals(POSITION_COLOR_TEXTURE, batch.getInt(ELEMENTS_OFFSET));
        assertEquals(ALPHA_BLENDING, batch.getInt(BLENDING_OFFSET));
        assertEquals(TESTS_AND_WRITES_DEPTH, batch.getInt(DEPTH_OFFSET));
        assertEquals(CULLING_DISABLED, batch.getInt(CULLING_OFFSET));
        assertEquals(TEXTURE_SHADING, batch.getInt(SHADING_OFFSET));
        assertEquals(GREEN, batch.getInt(VERTICES_OFFSET + 12));
        assertEquals(0.5f, batch.getFloat(VERTICES_OFFSET + 16));
        assertEquals(0, recorder.approximatedDraws());
    }

    @Test
    void blendFactorsTheSceneDoesNotKnowAreDrawnWithAlphaAndCounted() {
        RenderState state = RenderState.builder().blend(DESTINATION_ALPHA, 1).build();

        recorder.draw(triangle(IDENTITY).state(state).build());

        assertEquals(ALPHA_BLENDING, batchOf(written(recorder), 2).getInt(BLENDING_OFFSET));
        assertEquals(1, recorder.approximatedDraws());
    }

    @Test
    void aDrawThatDoesNotFitTheStreamOfTheFrameIsDroppedAndCounted() {
        DrawRecorder small = new DrawRecorder(600, 8);

        assertTrue(small.draw(triangle(IDENTITY).build()));
        assertFalse(small.draw(triangle(IDENTITY).build()));

        assertEquals(1, small.droppedDraws());
        assertEquals(
                Arrays.asList(
                        SceneOpcode.BEGIN_FRAME, SceneOpcode.SET_VIEW, SceneOpcode.DRAW_BATCH, SceneOpcode.END_FRAME),
                written(small).opcodes());
    }

    @Test
    void aFrameHasAsManyViewsAsTheSceneTakes() {
        DrawRecorder roomy = new DrawRecorder(48 * 1024, 128);
        for (int view = 0; view < 32; view++) {
            float[] projection = IDENTITY.clone();
            projection[12] = view;
            assertTrue(roomy.draw(triangle(projection).build()));
        }

        assertFalse(roomy.draw(triangle(HALVED).build()));
        assertEquals(1, roomy.droppedDraws());
    }

    @Test
    void beginningAFrameForgetsTheOneBefore() {
        recorder.enterWorld();
        recorder.draw(triangle(IDENTITY).build());

        recorder.beginFrame();

        assertEquals(Arrays.asList(SceneOpcode.BEGIN_FRAME, SceneOpcode.END_FRAME), written(recorder).opcodes());
    }

    @Test
    void aDrawWithoutVerticesLeavesNoBatch() {
        ByteBuffer nothing = ByteBuffer.allocate(0);

        assertTrue(recorder.draw(GameDraw.builder(POSITION_COLOR, PrimitiveMode.QUADS, nothing, 0)
                .view(GameView.builder(IDENTITY).build())
                .build()));

        assertEquals(Arrays.asList(SceneOpcode.BEGIN_FRAME, SceneOpcode.END_FRAME), written(recorder).opcodes());
    }

    @Test
    void fogThatEndsBeforeItStartsIsPushedOutOfSight() {
        GameView view = GameView.builder(IDENTITY)
                .fog(new float[] {1.0f, 1.0f, 1.0f, 1.0f}, Float.MAX_VALUE, 0.0f, FogShape.SPHERE)
                .build();

        recorder.draw(triangle(IDENTITY).view(view).build());

        assertEquals(SceneOpcode.SET_VIEW, written(recorder).opcodes().get(1));
    }

    @Test
    void aDepthClearIsWrittenBeforeTheDrawThatFollowsIt() {
        recorder.draw(triangle(IDENTITY).build());
        recorder.clearDepth();
        recorder.draw(triangle(IDENTITY).build());

        assertEquals(
                Arrays.asList(
                        SceneOpcode.BEGIN_FRAME,
                        SceneOpcode.SET_VIEW,
                        SceneOpcode.DRAW_BATCH,
                        SceneOpcode.CLEAR_DEPTH,
                        SceneOpcode.DRAW_BATCH,
                        SceneOpcode.END_FRAME),
                written(recorder).opcodes());
    }

    @Test
    void depthThatIsAlreadyClearIsNotClearedAgain() {
        recorder.clearDepth();
        recorder.draw(triangle(IDENTITY).build());
        recorder.clearDepth();
        recorder.clearDepth();

        assertEquals(
                Arrays.asList(
                        SceneOpcode.BEGIN_FRAME,
                        SceneOpcode.SET_VIEW,
                        SceneOpcode.DRAW_BATCH,
                        SceneOpcode.CLEAR_DEPTH,
                        SceneOpcode.END_FRAME),
                written(recorder).opcodes());
    }

    @Test
    void aBlurIsWrittenAfterTheDrawsItSpreads() {
        recorder.draw(triangle(IDENTITY).build());
        assertTrue(recorder.blur(new FrameBlur(FIVE_PIXELS, THRICE)));
        recorder.draw(triangle(IDENTITY).build());

        RecordedStream stream = written(recorder);

        assertEquals(
                Arrays.asList(
                        SceneOpcode.BEGIN_FRAME,
                        SceneOpcode.SET_VIEW,
                        SceneOpcode.DRAW_BATCH,
                        SceneOpcode.BLUR_FRAME,
                        SceneOpcode.DRAW_BATCH,
                        SceneOpcode.END_FRAME),
                stream.opcodes());
        assertEquals(FIVE_PIXELS, stream.payload(3).getFloat(0));
        assertEquals(THRICE, stream.payload(3).getInt(4));
    }

    @Test
    void aBlurAfterTheLastDrawStillReachesTheFrame() {
        recorder.draw(triangle(IDENTITY).build());
        recorder.blur(new FrameBlur(FIVE_PIXELS, THRICE));

        assertEquals(
                Arrays.asList(
                        SceneOpcode.BEGIN_FRAME,
                        SceneOpcode.SET_VIEW,
                        SceneOpcode.DRAW_BATCH,
                        SceneOpcode.BLUR_FRAME,
                        SceneOpcode.END_FRAME),
                written(recorder).opcodes());
    }

    @Test
    void beginningAFrameForgetsTheEffectsOfTheOneBefore() {
        recorder.draw(triangle(IDENTITY).build());
        recorder.clearDepth();
        recorder.blur(new FrameBlur(FIVE_PIXELS, THRICE));
        recorder.beginFrame();

        assertEquals(Arrays.asList(SceneOpcode.BEGIN_FRAME, SceneOpcode.END_FRAME), written(recorder).opcodes());
    }

    @Test
    void aBlurThatDoesNotFitTheStreamOfTheFrameIsDroppedAndCounted() {
        DrawRecorder full = new DrawRecorder(SceneOpcode.BEGIN_FRAME.fixedBytes() + 64, 2);

        assertFalse(full.blur(new FrameBlur(FIVE_PIXELS, THRICE)));
        assertEquals(1, full.droppedDraws());
    }

    @Test
    void aPartHoldsWhatWasRecordedAndLeavesTheFrameOpen() {
        recorder.draw(triangle(IDENTITY).build());

        assertEquals(
                Arrays.asList(SceneOpcode.BEGIN_FRAME, SceneOpcode.SET_VIEW, SceneOpcode.DRAW_BATCH),
                writtenPart(recorder).opcodes());
        assertTrue(recorder.isPartlyWritten());
    }

    @Test
    void theRestOfAFrameFollowsItsPartWithoutBeginningAgain() {
        recorder.draw(triangle(IDENTITY).build());
        writtenPart(recorder);
        recorder.draw(triangle(IDENTITY).build());
        recorder.draw(triangle(HALVED).build());

        RecordedStream rest = written(recorder);

        assertEquals(
                Arrays.asList(
                        SceneOpcode.SET_VIEW, SceneOpcode.DRAW_BATCH, SceneOpcode.DRAW_BATCH, SceneOpcode.END_FRAME),
                rest.opcodes());
        assertEquals(0, rest.payload(1).getInt(VIEW_OFFSET));
        assertEquals(1, rest.payload(2).getInt(VIEW_OFFSET));
    }

    @Test
    void optionsLeftByAPartAreNotWrittenAgain() {
        recorder.draw(triangle(IDENTITY).clip(0, 0, 8, 8).build());
        writtenPart(recorder);
        recorder.draw(triangle(IDENTITY).clip(0, 0, 8, 8).build());
        recorder.draw(triangle(IDENTITY).build());

        assertEquals(
                Arrays.asList(
                        SceneOpcode.DRAW_BATCH, SceneOpcode.CLEAR_CLIP, SceneOpcode.DRAW_BATCH, SceneOpcode.END_FRAME),
                written(recorder).opcodes());
    }

    @Test
    void aPartWrittenInTheSkyLeavesTheTerrainToTheNextOne() {
        Fog fog = new Fog(0.5f, 0.6f, 0.7f, 1.0f, 16.0f, 64.0f, FogShape.CYLINDER);
        recorder.enterWorld();
        recorder.terrain(new TerrainState(IDENTITY, 0.0, 64.0, 0.0, fog, ATLAS, LIGHTMAP));
        recorder.draw(triangle(IDENTITY).build());

        assertEquals(
                Arrays.asList(SceneOpcode.BEGIN_FRAME, SceneOpcode.SET_VIEW, SceneOpcode.DRAW_BATCH),
                writtenPart(recorder).opcodes());

        recorder.enter(BatchSlot.SOLID);
        recorder.draw(triangle(IDENTITY).build());

        assertEquals(
                Arrays.asList(SceneOpcode.SET_TERRAIN, SceneOpcode.DRAW_BATCH, SceneOpcode.END_FRAME),
                written(recorder).opcodes());
    }

    @Test
    void aPartWrittenPastTheSkyCarriesTheTerrainOnce() {
        Fog fog = new Fog(0.5f, 0.6f, 0.7f, 1.0f, 16.0f, 64.0f, FogShape.CYLINDER);
        recorder.enterWorld();
        recorder.terrain(new TerrainState(IDENTITY, 0.0, 64.0, 0.0, fog, ATLAS, LIGHTMAP));
        recorder.enter(BatchSlot.SOLID);
        recorder.draw(triangle(IDENTITY).build());

        assertEquals(
                Arrays.asList(
                        SceneOpcode.BEGIN_FRAME, SceneOpcode.SET_VIEW, SceneOpcode.SET_TERRAIN, SceneOpcode.DRAW_BATCH),
                writtenPart(recorder).opcodes());
        assertEquals(Collections.singletonList(SceneOpcode.END_FRAME), written(recorder).opcodes());
    }

    @Test
    void depthIsClearedAgainAfterAPart() {
        recorder.draw(triangle(IDENTITY).build());
        recorder.clearDepth();
        writtenPart(recorder);
        recorder.clearDepth();

        assertEquals(Arrays.asList(SceneOpcode.CLEAR_DEPTH, SceneOpcode.END_FRAME), written(recorder).opcodes());
    }

    @Test
    void eachPartHasTheRoomOfAStream() {
        DrawRecorder narrow = new DrawRecorder(32 * 1024, 4);

        assertTrue(narrow.draw(triangle(IDENTITY).build()));
        assertFalse(narrow.draw(triangle(IDENTITY).build()));
        writtenPart(narrow);

        assertTrue(narrow.draw(triangle(IDENTITY).build()));
    }

    @Test
    void theWorldIsRefusedOnceAPartWasWrittenWithoutIt() {
        writtenPart(recorder);

        assertThrows(IllegalStateException.class, recorder::enterWorld);
    }

    @Test
    void beginningAFrameForgetsThePartsOfTheOneBefore() {
        recorder.draw(triangle(IDENTITY).build());
        writtenPart(recorder);
        recorder.beginFrame();

        assertFalse(recorder.isPartlyWritten());
        assertEquals(Arrays.asList(SceneOpcode.BEGIN_FRAME, SceneOpcode.END_FRAME), written(recorder).opcodes());
    }

    private static GameDraw.Builder triangle(float[] projection) {
        ByteBuffer vertices = ByteBuffer.allocate(48).order(ByteOrder.LITTLE_ENDIAN);
        for (int corner = 0; corner < 3; corner++) {
            vertices.putFloat(corner).putFloat(0.0f).putFloat(0.0f).putInt(GREEN);
        }
        vertices.flip();
        return GameDraw.builder(POSITION_COLOR, PrimitiveMode.TRIANGLES, vertices, 3)
                .view(GameView.builder(projection).build());
    }

    private static RecordedStream writtenPart(DrawRecorder frame) {
        RecordedStream stream = new RecordedStream();
        frame.writePart(stream.scene());
        return stream;
    }

    private static RecordedStream written(DrawRecorder frame) {
        RecordedStream stream = new RecordedStream();
        frame.write(stream.scene());
        return stream;
    }

    private static ByteBuffer batchOf(RecordedStream stream, int record) {
        stream.opcodes();
        return stream.payload(record);
    }
}
