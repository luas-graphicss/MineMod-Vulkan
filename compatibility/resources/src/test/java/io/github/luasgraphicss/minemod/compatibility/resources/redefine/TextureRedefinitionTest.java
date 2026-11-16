package io.github.luasgraphicss.minemod.compatibility.resources.redefine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import io.github.luasgraphicss.minemod.compatibility.resources.alias.VanillaAliases;
import io.github.luasgraphicss.minemod.compatibility.resources.names.NameTable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class TextureRedefinitionTest {
    private static final long LIGHTMAP = 0x2002L;
    private static final int SHARED_IMPORT = 70;
    private static final int PRIVATE_STORAGE = 71;

    private final NameTable textures = new NameTable(ObjectKind.TEXTURE, 8);
    private final VanillaAliases aliases = new VanillaAliases(textures);
    private final List<Integer> disposed = new ArrayList<>();
    private final RecordingBackend backend = new RecordingBackend();
    private final TextureRedefinition redefinition =
            new TextureRedefinition(textures, aliases, backend, (kind, backing) -> disposed.add(backing));
    private final int name = aliases.publish(LIGHTMAP, SHARED_IMPORT, (kind, backing) -> disposed.add(backing));

    @Test
    void theNameMovesToPrivateStorageAndItsReferencesFollow() {
        assertEquals(PRIVATE_STORAGE, redefinition.redefine(name));

        assertEquals(PRIVATE_STORAGE, textures.backingOf(name));
        assertEquals(Collections.singletonList("redirect 70 to 71"), backend.calls);
        assertFalse(aliases.isAlias(name));
        assertEquals(Collections.singletonList(SHARED_IMPORT), disposed);
    }

    @Test
    void aSharedImportStillUsedByAFrameInFlightOutlivesTheRedefinition() {
        textures.markUsed(name, 12);

        redefinition.redefine(name);

        assertEquals(Collections.emptyList(), disposed);
        assertEquals(1, textures.retiringCount());
        textures.collect(12, (kind, backing) -> disposed.add(backing));
        assertEquals(Collections.singletonList(SHARED_IMPORT), disposed);
    }

    @Test
    void aFailedRedirectLeavesTheSharedStorageInPlace() {
        backend.failingRedirect = true;

        assertThrows(IllegalStateException.class, () -> redefinition.redefine(name));

        assertEquals(SHARED_IMPORT, textures.backingOf(name));
        assertTrue(aliases.isAlias(name));
        assertEquals(Arrays.asList("redirect 70 to 71", "discard 71"), backend.calls);
        assertEquals(Collections.emptyList(), disposed);
    }

    @Test
    void aFailedAllocationChangesNothing() {
        backend.failingAllocation = true;

        assertThrows(IllegalStateException.class, () -> redefinition.redefine(name));

        assertEquals(SHARED_IMPORT, textures.backingOf(name));
        assertEquals(Collections.emptyList(), backend.calls);
    }

    @Test
    void onlyAliasesAreRedefinedThisWay() {
        int ordinary = textures.allocate(90);

        assertThrows(IllegalArgumentException.class, () -> redefinition.redefine(ordinary));
    }

    private static final class RecordingBackend implements RedefinitionBackend {
        private final List<String> calls = new ArrayList<>();
        private boolean failingAllocation;
        private boolean failingRedirect;

        @Override
        public int allocatePrivateStorage() {
            if (failingAllocation) {
                throw new IllegalStateException("the provider is out of memory");
            }
            return PRIVATE_STORAGE;
        }

        @Override
        public void redirectReferences(int fromBacking, int toBacking) {
            calls.add("redirect " + fromBacking + " to " + toBacking);
            if (failingRedirect) {
                throw new IllegalStateException("an attachment could not be replaced");
            }
        }

        @Override
        public void discard(int backing) {
            calls.add("discard " + backing);
        }
    }
}
