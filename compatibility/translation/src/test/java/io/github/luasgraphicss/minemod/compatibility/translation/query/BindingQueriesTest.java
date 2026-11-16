package io.github.luasgraphicss.minemod.compatibility.translation.query;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import io.github.luasgraphicss.minemod.compatibility.provider.capability.ContextProfile;
import io.github.luasgraphicss.minemod.compatibility.resources.framebuffer.DefaultFramebufferBuffers;
import io.github.luasgraphicss.minemod.compatibility.resources.framebuffer.FramebufferBindings;
import io.github.luasgraphicss.minemod.compatibility.resources.framebuffer.FramebufferTarget;
import io.github.luasgraphicss.minemod.compatibility.translation.legacy.DisplayListNames;
import io.github.luasgraphicss.minemod.compatibility.translation.names.ObjectNames;
import io.github.luasgraphicss.minemod.compatibility.translation.support.LayerFixture;
import io.github.luasgraphicss.minemod.compatibility.translation.support.RecordingProvider;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class BindingQueriesTest {
    private static final int GL_NONE = 0;
    private static final int GL_BACK = 0x0405;
    private static final int GL_LIST_BASE = 0x0B32;
    private static final int GL_DRAW_BUFFER = 0x0C01;
    private static final int GL_MAX_TEXTURE_SIZE = 0x0D33;
    private static final int GL_TEXTURE_BINDING_2D = 0x8069;
    private static final int GL_CURRENT_PROGRAM = 0x8B8D;
    private static final int GL_DRAW_FRAMEBUFFER_BINDING = 0x8CA6;
    private static final int GL_READ_FRAMEBUFFER_BINDING = 0x8CAA;

    private final LayerFixture fixture = new LayerFixture();
    private final ObjectNames names = fixture.names(ContextProfile.COMPATIBILITY);
    private final FramebufferBindings framebuffers =
            new FramebufferBindings(fixture.contextNames.table(ObjectKind.FRAMEBUFFER), 7);
    private final DefaultFramebufferBuffers defaultBuffers = new DefaultFramebufferBuffers();
    private final DisplayListNames lists =
            new DisplayListNames(() -> fixture.contextNames, fixture.provider, fixture.errors, () -> fixture.frame);
    private final BindingQueries queries = new BindingQueries(names, framebuffers, defaultBuffers, lists);

    @Test
    void framebufferBindingsAnswerWithTheModsNamesAndNeverTheScene() {
        int offscreen = names.generate(ObjectKind.FRAMEBUFFER, 1)[0];
        framebuffers.bind(FramebufferTarget.DRAW_FRAMEBUFFER, offscreen);

        assertEquals(OptionalInt.of(offscreen), queries.answeredHere(GL_DRAW_FRAMEBUFFER_BINDING));
        assertEquals(OptionalInt.of(0), queries.answeredHere(GL_READ_FRAMEBUFFER_BINDING));
    }

    @Test
    void theDrawBufferOfTheDefaultFramebufferIsTheSelectorTheModChose() {
        assertEquals(OptionalInt.of(GL_BACK), queries.answeredHere(GL_DRAW_BUFFER));

        defaultBuffers.selectDraw(GL_NONE);
        assertEquals(OptionalInt.of(GL_NONE), queries.answeredHere(GL_DRAW_BUFFER));

        framebuffers.bind(FramebufferTarget.FRAMEBUFFER, names.generate(ObjectKind.FRAMEBUFFER, 1)[0]);
        assertEquals(OptionalInt.empty(), queries.answeredHere(GL_DRAW_BUFFER));
    }

    @Test
    void theListBaseIsKeptHere() {
        lists.setBase(64);

        assertEquals(OptionalInt.of(64), queries.answeredHere(GL_LIST_BASE));
    }

    @Test
    void aBoundObjectIsReportedUnderTheModsName() {
        int texture = names.generate(ObjectKind.TEXTURE, 2)[1];
        int program = names.create(ObjectKind.SHADER_OR_PROGRAM, 0);

        assertEquals(texture, queries.toVisibleValue(GL_TEXTURE_BINDING_2D, RecordingProvider.FIRST_BACKING + 1));
        assertEquals(program, queries.toVisibleValue(GL_CURRENT_PROGRAM, RecordingProvider.FIRST_BACKING + 2));
        assertEquals(0, queries.toVisibleValue(GL_TEXTURE_BINDING_2D, 0));
    }

    @Test
    void otherValuesPassThroughUnchanged() {
        assertEquals(OptionalInt.empty(), queries.answeredHere(GL_MAX_TEXTURE_SIZE));
        assertEquals(16384, queries.toVisibleValue(GL_MAX_TEXTURE_SIZE, 16384));
    }
}
