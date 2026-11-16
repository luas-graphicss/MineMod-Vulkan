package io.github.luasgraphicss.minemod.compatibility.resources.alias;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import io.github.luasgraphicss.minemod.compatibility.resources.names.NameTable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

class VanillaAliasesTest {
    private static final long BLOCK_ATLAS = 0x1001L;
    private static final int FIRST_IMPORT = 40;
    private static final int SECOND_IMPORT = 41;

    private final NameTable textures = new NameTable(ObjectKind.TEXTURE, 8);
    private final VanillaAliases aliases = new VanillaAliases(textures);
    private final List<Integer> disposed = new ArrayList<>();

    @Test
    void aVanillaResourceIsVisibleUnderAnOrdinaryTextureName() {
        int name = publish(FIRST_IMPORT);

        assertTrue(aliases.isAlias(name));
        assertEquals(FIRST_IMPORT, textures.backingOf(name));
        assertEquals(OptionalLong.of(BLOCK_ATLAS), aliases.resourceOf(name));
        assertEquals(OptionalInt.of(name), aliases.nameOf(BLOCK_ATLAS));
    }

    @Test
    void aReloadedResourceKeepsItsNameAndRetiresTheOldImportAfterItsLastFrame() {
        int name = publish(FIRST_IMPORT);
        textures.markUsed(name, 7);

        assertEquals(name, publish(SECOND_IMPORT));

        assertEquals(SECOND_IMPORT, textures.backingOf(name));
        assertEquals(Collections.emptyList(), disposed);
        textures.collect(6, this::dispose);
        assertEquals(Collections.emptyList(), disposed);
        textures.collect(7, this::dispose);
        assertEquals(Collections.singletonList(FIRST_IMPORT), disposed);
    }

    @Test
    void aWithdrawnResourceFreesItsName() {
        int name = publish(FIRST_IMPORT);

        assertTrue(aliases.withdraw(BLOCK_ATLAS, this::dispose));

        assertFalse(textures.contains(name));
        assertFalse(aliases.isAlias(name));
        assertEquals(Collections.singletonList(FIRST_IMPORT), disposed);
        assertFalse(aliases.withdraw(BLOCK_ATLAS, this::dispose));
    }

    @Test
    void aDetachedNameStaysLiveButNoLongerFollowsTheResource() {
        int name = publish(FIRST_IMPORT);

        assertTrue(aliases.detach(name));

        assertTrue(textures.contains(name));
        assertEquals(0, aliases.aliasCount());
        assertEquals(OptionalInt.empty(), aliases.nameOf(BLOCK_ATLAS));
        assertFalse(aliases.detach(name));
    }

    @Test
    void aNameTheModDeletedAndGotBackIsNoLongerAnAlias() {
        int name = publish(FIRST_IMPORT);
        textures.delete(name, this::dispose);

        assertEquals(name, textures.allocate(SECOND_IMPORT));

        assertFalse(aliases.isAlias(name));
        assertEquals(OptionalInt.empty(), aliases.nameOf(BLOCK_ATLAS));
        assertEquals(0, aliases.aliasCount());
    }

    @Test
    void onlyTextureNamesCanAliasVanillaResources() {
        assertThrows(IllegalArgumentException.class, () -> new VanillaAliases(new NameTable(ObjectKind.BUFFER, 8)));
    }

    private int publish(int importedBacking) {
        return aliases.publish(BLOCK_ATLAS, importedBacking, this::dispose);
    }

    private void dispose(ObjectKind kind, int backing) {
        disposed.add(backing);
    }
}
