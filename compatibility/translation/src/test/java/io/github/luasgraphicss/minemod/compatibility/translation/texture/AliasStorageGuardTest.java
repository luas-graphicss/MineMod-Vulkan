package io.github.luasgraphicss.minemod.compatibility.translation.texture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import io.github.luasgraphicss.minemod.compatibility.contracts.support.OperationSupport;
import io.github.luasgraphicss.minemod.compatibility.resources.alias.VanillaAliases;
import io.github.luasgraphicss.minemod.compatibility.resources.names.NameTable;
import io.github.luasgraphicss.minemod.compatibility.resources.redefine.RedefinitionBackend;
import io.github.luasgraphicss.minemod.compatibility.resources.redefine.TextureRedefinition;
import io.github.luasgraphicss.minemod.compatibility.translation.support.LayerFixture;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class AliasStorageGuardTest {
    private static final long BLOCK_ATLAS = 0x1001L;
    private static final int SHARED_IMPORT = 300;
    private static final int PRIVATE_STORAGE = 500;
    private static final int GL_TEXTURE_2D = 0x0DE1;
    private static final int GL_TEXTURE1 = 0x84C1;
    private static final int GL_TEXTURE_CUBE_MAP = 0x8513;
    private static final int GL_TEXTURE_CUBE_MAP_NEGATIVE_Y = 0x8518;

    private final LayerFixture fixture = new LayerFixture();
    private final NameTable textures = fixture.contextNames.table(ObjectKind.TEXTURE);
    private final VanillaAliases aliases = new VanillaAliases(textures);
    private final TextureBindings bindings = new TextureBindings();
    private final RedefinitionBackend backend = new RedefinitionBackend() {
        @Override
        public int allocatePrivateStorage() {
            return PRIVATE_STORAGE;
        }

        @Override
        public void redirectReferences(int fromBacking, int toBacking) {
            fixture.provider.invoke("redirect", new long[] {fromBacking, toBacking});
        }

        @Override
        public void discard(int backing) {
            fixture.provider.invoke("discard", new long[] {backing});
        }
    };
    private final AliasStorageGuard guard = new AliasStorageGuard(
            bindings,
            aliases,
            new TextureRedefinition(textures, aliases, backend, fixture.provider),
            fixture.provider,
            fixture.reporter);
    private final int atlas = aliases.publish(BLOCK_ATLAS, SHARED_IMPORT, fixture.provider);

    @Test
    void redefiningAVanillaTextureMovesTheNameToPrivateStorageFirst() {
        bindings.observe("glBindTexture", new long[] {GL_TEXTURE_2D, atlas});

        guard.observe("glTexImage2D", new long[] {GL_TEXTURE_2D, 0, 0x8058, 64, 64, 0, 0x1908, 0x1401, 0});

        assertEquals(PRIVATE_STORAGE, textures.backingOf(atlas));
        assertFalse(aliases.isAlias(atlas));
        assertEquals(
                Arrays.asList("redirect[300, 500]", "glBindTexture[3553, 500]"),
                fixture.provider.events());
        assertEquals(OperationSupport.BRIDGE_REQUIRED, fixture.onlyReport().support());
        assertEquals("glTexImage2D on a vanilla texture", fixture.onlyReport().operation());
        assertEquals(atlas, fixture.onlyReport().resource().get().name());
    }

    @Test
    void writingIntoTheSharedStorageIsNotARedefinition() {
        bindings.observe("glBindTexture", new long[] {GL_TEXTURE_2D, atlas});

        guard.observe("glTexSubImage2D", new long[] {GL_TEXTURE_2D, 0, 0, 0, 16, 16, 0x1908, 0x1401, 0});

        assertTrue(aliases.isAlias(atlas));
        assertEquals(Collections.emptyList(), fixture.provider.events());
    }

    @Test
    void aCubeFaceRedefinesTheTextureBoundToTheCubeMapTarget() {
        bindings.observe("glBindTexture", new long[] {GL_TEXTURE_CUBE_MAP, atlas});

        guard.observe("glTexImage2D", new long[] {GL_TEXTURE_CUBE_MAP_NEGATIVE_Y, 0, 0x8058, 64, 64, 0, 0x1908, 0x1401, 0});

        assertEquals("glBindTexture[34067, 500]", fixture.provider.events().get(1));
    }

    @Test
    void onlyTheTextureOfTheActiveUnitIsRedefined() {
        bindings.observe("glBindTexture", new long[] {GL_TEXTURE_2D, atlas});
        bindings.observe("glActiveTexture", new long[] {GL_TEXTURE1});

        guard.observe("glTexImage2D", new long[] {GL_TEXTURE_2D, 0, 0x8058, 64, 64, 0, 0x1908, 0x1401, 0});

        assertTrue(aliases.isAlias(atlas));
        assertEquals(SHARED_IMPORT, textures.backingOf(atlas));
    }

    @Test
    void anOrdinaryTextureKeepsItsStorage() {
        int ordinary = textures.allocate(900);
        bindings.observe("glBindTexture", new long[] {GL_TEXTURE_2D, ordinary});

        guard.observe("glTexStorage2D", new long[] {GL_TEXTURE_2D, 1, 0x8058, 64, 64});

        assertEquals(900, textures.backingOf(ordinary));
        assertEquals(0, fixture.reports.reportedOperations().size());
    }
}
