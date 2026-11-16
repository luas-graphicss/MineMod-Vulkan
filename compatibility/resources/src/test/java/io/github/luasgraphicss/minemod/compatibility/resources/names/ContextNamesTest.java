package io.github.luasgraphicss.minemod.compatibility.resources.names;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectScope;
import org.junit.jupiter.api.Test;

class ContextNamesTest {
    private final NameSpace shared = new NameSpace(ObjectScope.SHARE_GROUP, 8);
    private final ContextNames first = new ContextNames(shared, new NameSpace(ObjectScope.CONTEXT, 8));
    private final ContextNames second = new ContextNames(shared, new NameSpace(ObjectScope.CONTEXT, 8));

    @Test
    void contextsOfOneShareGroupSeeTheSameTextures() {
        int texture = first.table(ObjectKind.TEXTURE).allocate(101);

        assertEquals(101, second.table(ObjectKind.TEXTURE).backingOf(texture));
    }

    @Test
    void framebuffersStayPrivateToTheirContext() {
        int framebuffer = first.table(ObjectKind.FRAMEBUFFER).allocate(201);

        assertFalse(second.table(ObjectKind.FRAMEBUFFER).contains(framebuffer));
    }

    @Test
    void countsLiveObjectsAcrossKinds() {
        first.table(ObjectKind.TEXTURE).allocate(101);
        second.table(ObjectKind.BUFFER).allocate(102);

        assertEquals(2, shared.liveCount());
        assertEquals(2, shared.discardAll());
        assertEquals(0, shared.liveCount());
    }

    @Test
    void rejectsNameSpacesOfTheWrongScope() {
        NameSpace local = new NameSpace(ObjectScope.CONTEXT, 8);

        assertThrows(IllegalArgumentException.class, () -> new ContextNames(local, local));
        assertThrows(IllegalArgumentException.class, () -> new ContextNames(shared, shared));
        assertThrows(IllegalArgumentException.class, () -> shared.table(ObjectKind.FRAMEBUFFER));
    }
}
