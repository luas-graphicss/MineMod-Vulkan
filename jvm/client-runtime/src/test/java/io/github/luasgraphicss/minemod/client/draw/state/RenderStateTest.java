package io.github.luasgraphicss.minemod.client.draw.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.codec.scene.Blending;
import io.github.luasgraphicss.minemod.codec.scene.DepthCompare;
import io.github.luasgraphicss.minemod.codec.scene.Shading;
import io.github.luasgraphicss.minemod.codec.scene.VertexElement;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RenderStateTest {
    private static final int ZERO = 0;
    private static final int ONE = 1;
    private static final int SOURCE_COLOR = 0x0300;
    private static final int ONE_MINUS_SOURCE_COLOR = 0x0301;
    private static final int SOURCE_ALPHA = 0x0302;
    private static final int ONE_MINUS_SOURCE_ALPHA = 0x0303;
    private static final int DESTINATION_ALPHA = 0x0304;
    private static final int DESTINATION_COLOR = 0x0306;
    private static final int ONE_MINUS_DESTINATION_COLOR = 0x0307;
    private static final int DEPTH_LESS = 0x0201;
    private static final int DEPTH_EQUAL = 0x0202;
    private static final int DEPTH_LESS_OR_EQUAL = 0x0203;
    private static final int DEPTH_GREATER = 0x0204;
    private static final int DEPTH_ALWAYS = 0x0207;

    @Test
    void theBlendFactorsOfTheGameNameTheBlendingOfTheScene() {
        assertEquals(Optional.of(Blending.ALPHA), BlendTranslation.of(SOURCE_ALPHA, ONE_MINUS_SOURCE_ALPHA));
        assertEquals(Optional.of(Blending.ADDITIVE), BlendTranslation.of(ONE, ONE));
        assertEquals(Optional.of(Blending.LIGHTNING), BlendTranslation.of(SOURCE_ALPHA, ONE));
        assertEquals(Optional.of(Blending.GLINT), BlendTranslation.of(SOURCE_COLOR, ONE));
        assertEquals(Optional.of(Blending.CRUMBLING), BlendTranslation.of(DESTINATION_COLOR, SOURCE_COLOR));
        assertEquals(
                Optional.of(Blending.INVERSION),
                BlendTranslation.of(ONE_MINUS_DESTINATION_COLOR, ONE_MINUS_SOURCE_COLOR));
        assertEquals(Optional.of(Blending.DARKENING), BlendTranslation.of(ZERO, ONE_MINUS_SOURCE_COLOR));
        assertEquals(Optional.of(Blending.DISABLED), BlendTranslation.of(ONE, ZERO));
    }

    @Test
    void blendFactorsTheSceneDoesNotKnowHaveNoBlending() {
        assertEquals(Optional.empty(), BlendTranslation.of(DESTINATION_ALPHA, ONE));
    }

    @Test
    void theDepthFunctionsOfTheGameNameTheComparisonsOfTheScene() {
        assertEquals(Optional.of(DepthCompare.LESS_OR_EQUAL), DepthTranslation.of(DEPTH_LESS_OR_EQUAL));
        assertEquals(Optional.of(DepthCompare.EQUAL), DepthTranslation.of(DEPTH_EQUAL));
        assertEquals(Optional.of(DepthCompare.GREATER), DepthTranslation.of(DEPTH_GREATER));
        assertEquals(Optional.of(DepthCompare.ALWAYS), DepthTranslation.of(DEPTH_ALWAYS));
        assertEquals(Optional.empty(), DepthTranslation.of(DEPTH_LESS));
    }

    @Test
    void aStateIsExactWhenTheSceneKnowsItsBlendingAndItsDepthTest() {
        assertTrue(RenderState.builder().build().isExact());
        assertTrue(RenderState.builder()
                .blend(SOURCE_ALPHA, ONE_MINUS_SOURCE_ALPHA)
                .depthTest(DEPTH_LESS_OR_EQUAL)
                .build()
                .isExact());
        assertFalse(RenderState.builder().blend(DESTINATION_ALPHA, ONE).build().isExact());
        assertFalse(RenderState.builder().depthTest(DEPTH_LESS).build().isExact());
    }

    @Test
    void shadingKeepsTheStepsTheVerticesCanFeed() {
        Set<Shading> asked = EnumSet.allOf(Shading.class);
        Set<VertexElement> positionAndTexture = EnumSet.of(VertexElement.POSITION, VertexElement.TEXTURE);

        assertEquals(EnumSet.of(Shading.TEXTURE, Shading.FOG), ShadingSupport.of(asked, positionAndTexture, true));
        assertEquals(EnumSet.of(Shading.FOG), ShadingSupport.of(asked, positionAndTexture, false));
        assertEquals(asked, ShadingSupport.of(asked, EnumSet.allOf(VertexElement.class), true));
    }
}
