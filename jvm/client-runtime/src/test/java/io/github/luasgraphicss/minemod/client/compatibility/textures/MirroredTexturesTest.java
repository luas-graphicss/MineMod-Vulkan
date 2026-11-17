package io.github.luasgraphicss.minemod.client.compatibility.textures;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.client.textures.TexelArea;
import io.github.luasgraphicss.minemod.client.textures.TextureShape;
import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import io.github.luasgraphicss.minemod.compatibility.layer.fixtures.HeapMemory;
import io.github.luasgraphicss.minemod.compatibility.layer.fixtures.RecordingProvider;
import io.github.luasgraphicss.minemod.compatibility.resources.alias.VanillaAliases;
import io.github.luasgraphicss.minemod.compatibility.resources.names.NameTable;
import io.github.luasgraphicss.minemod.compatibility.resources.names.ObjectDisposer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MirroredTexturesTest {
    private static final int GL_UNPACK_ROW_LENGTH = 0x0CF2;
    private static final int GL_UNPACK_ALIGNMENT = 0x0CF5;
    private static final int GL_MAX_TEXTURE_SIZE = 0x0D33;
    private static final int GL_TEXTURE_BINDING_2D = 0x8069;
    private static final int GL_PIXEL_UNPACK_BUFFER_BINDING = 0x88EF;
    private static final int GL_TEXTURE_MIN_FILTER = 0x2801;
    private static final int GL_NEAREST = 0x2600;
    private static final int LARGEST_SIDE = 64;
    private static final int FIRST_COPY = RecordingProvider.FIRST_NAME;
    private static final int SECOND_COPY = RecordingProvider.FIRST_NAME + 1;
    private static final int TEXTURE_OF_A_MOD = 77;
    private static final int BUFFER_OF_A_MOD = 5;
    private static final int STORAGE_OF_A_MOD = 500;
    private static final TextureShape SMALL = new TextureShape(4, 2, 1);
    private static final TexelArea ALL_OF_SMALL = new TexelArea(0, 0, 0, 4, 2);

    private final HeapMemory memory = new HeapMemory();
    private final RecordingProvider provider = new RecordingProvider(memory);
    private final NameTable names = new NameTable(ObjectKind.TEXTURE, 16);
    private final VanillaAliases published = new VanillaAliases(names);
    private final List<Integer> disposed = new ArrayList<>();
    private final ObjectDisposer disposer = (kind, copy) -> disposed.add(copy);
    private MirroredTextures mirror;

    @BeforeEach
    void mirrorIntoAProviderWithSmallTextures() {
        provider.answer(GL_MAX_TEXTURE_SIZE, LARGEST_SIDE);
        mirror = new MirroredTextures(new PublishedNames(), new ProviderTextures(provider, memory));
    }

    @Test
    void aTextureOfTheGameIsNamedForModsOverACopyInTheProvider() {
        int texture = mirror.generate();

        assertTrue(published.isAlias(texture));
        assertEquals(FIRST_COPY, names.backingOf(texture));
        assertEquals(1, mirror.mirroredCount());
    }

    @Test
    void theCopyReceivesTheParametersTheStorageAndTheTexelsOfTheTexture() {
        byte[] texels = texelsOf(ALL_OF_SMALL);
        int texture = mirror.generate();

        mirror.setParameter(texture, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        mirror.define(texture, SMALL);
        mirror.write(texture, ALL_OF_SMALL, ByteBuffer.wrap(texels));

        assertEquals(
                Arrays.asList(
                        "glTexParameteri[3553, 10241, 9728]",
                        "glTexStorage2D[3553, 1, 32856, 4, 2]",
                        "glTexSubImage2D[3553, 0, 0, 0, 4, 2, 6408, 5121"),
                callsOn(FIRST_COPY));
        assertArrayEquals(texels, provider.uploads().get(0));
        assertEquals(1, memory.liveAllocations());
    }

    @Test
    void whatAModBoundAndTheWayItUnpacksAreBackAfterAWrite() {
        int texture = mirror.generate();
        mirror.define(texture, SMALL);
        provider.answer(GL_TEXTURE_BINDING_2D, TEXTURE_OF_A_MOD);
        provider.answer(GL_PIXEL_UNPACK_BUFFER_BINDING, BUFFER_OF_A_MOD);
        provider.answer(GL_UNPACK_ALIGNMENT, 8);
        provider.answer(GL_UNPACK_ROW_LENGTH, 3);
        provider.events().clear();

        mirror.write(texture, ALL_OF_SMALL, ByteBuffer.wrap(texelsOf(ALL_OF_SMALL)));

        List<String> calls = provider.events();
        int upload = indexOf(calls, "glTexSubImage2D");
        assertTrue(calls.indexOf("glPixelStorei[3314, 0]") < upload, calls.toString());
        assertTrue(calls.indexOf("glPixelStorei[3317, 4]") < upload, calls.toString());
        assertTrue(calls.indexOf("glBindBuffer[35052, 0]") < upload, calls.toString());
        assertEquals(TEXTURE_OF_A_MOD, provider.integer(GL_TEXTURE_BINDING_2D));
        assertEquals(BUFFER_OF_A_MOD, provider.integer(GL_PIXEL_UNPACK_BUFFER_BINDING));
        assertEquals(8, provider.integer(GL_UNPACK_ALIGNMENT));
        assertEquals(3, provider.integer(GL_UNPACK_ROW_LENGTH));
    }

    @Test
    void aTextureTheGameRedefinesKeepsItsNameOverANewCopyWithItsParameters() {
        int texture = mirror.generate();
        mirror.setParameter(texture, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        mirror.define(texture, SMALL);

        mirror.define(texture, new TextureShape(8, 8, 2));

        assertEquals(SECOND_COPY, names.backingOf(texture));
        assertEquals(Collections.singletonList(FIRST_COPY), disposed);
        assertEquals(
                Arrays.asList("glTexParameteri[3553, 10241, 9728]", "glTexStorage2D[3553, 2, 32856, 8, 8]"),
                callsOn(SECOND_COPY));
    }

    @Test
    void texelsOutsideTheStorageOfTheCopyAreLeftOut() {
        int texture = mirror.generate();
        mirror.write(texture, ALL_OF_SMALL, ByteBuffer.wrap(texelsOf(ALL_OF_SMALL)));
        mirror.define(texture, SMALL);

        TexelArea secondLevel = new TexelArea(1, 0, 0, 2, 1);
        mirror.write(texture, secondLevel, ByteBuffer.wrap(texelsOf(secondLevel)));

        assertEquals(Collections.emptyList(), provider.uploads());
    }

    @Test
    void aShapeTheProviderCannotHoldGetsNoStorage() {
        int texture = mirror.generate();
        TextureShape tooWide = new TextureShape(2 * LARGEST_SIDE, 2, 1);
        TexelArea firstTexels = new TexelArea(0, 0, 0, 2, 2);

        mirror.define(texture, tooWide);
        mirror.write(texture, firstTexels, ByteBuffer.wrap(texelsOf(firstTexels)));

        assertEquals(-1, indexOf(provider.events(), "glTexStorage2D"));
        assertEquals(Collections.emptyList(), provider.uploads());
    }

    @Test
    void aTextureAModRedefinedNoLongerFollowsTheGame() {
        int texture = mirror.generate();
        mirror.define(texture, SMALL);
        published.detach(texture);
        names.rebind(texture, STORAGE_OF_A_MOD, disposer);

        mirror.write(texture, ALL_OF_SMALL, ByteBuffer.wrap(texelsOf(ALL_OF_SMALL)));
        mirror.delete(texture);

        assertEquals(Collections.emptyList(), provider.uploads());
        assertEquals(0, mirror.mirroredCount());
        assertEquals(STORAGE_OF_A_MOD, names.backingOf(texture));
    }

    @Test
    void aTextureTheGameDeletesIsWithdrawnFromMods() {
        int texture = mirror.generate();

        mirror.delete(texture);

        assertFalse(names.contains(texture));
        assertEquals(Collections.singletonList(FIRST_COPY), disposed);
        assertEquals(0, mirror.mirroredCount());
    }

    @Test
    void theNameOfADeletedTextureGoesToTheNextOneOverACopyOfItsOwn() {
        int deleted = mirror.generate();
        mirror.delete(deleted);

        int next = mirror.generate();

        assertEquals(deleted, next);
        assertNotEquals(FIRST_COPY, names.backingOf(next));
    }

    private List<String> callsOn(int copy) {
        List<String> calls = new ArrayList<>();
        boolean isBound = false;
        for (String call : provider.events()) {
            if (call.startsWith("glBindTexture[")) {
                isBound = call.equals("glBindTexture[3553, " + copy + "]");
            } else if (isBound) {
                calls.add(call.startsWith("glTexSubImage2D") ? call.substring(0, call.lastIndexOf(',')) : call);
            }
        }
        return calls.stream().filter(call -> call.startsWith("glTex")).collect(Collectors.toList());
    }

    private static int indexOf(List<String> calls, String function) {
        for (int index = 0; index < calls.size(); index++) {
            if (calls.get(index).startsWith(function)) {
                return index;
            }
        }
        return -1;
    }

    private static byte[] texelsOf(TexelArea area) {
        byte[] texels = new byte[(int) area.bytes()];
        for (int index = 0; index < texels.length; index++) {
            texels[index] = (byte) (index + 1);
        }
        return texels;
    }

    private final class PublishedNames implements TextureAliases {
        @Override
        public int publish(long resource, int providerTexture) {
            return published.publish(resource, providerTexture, disposer);
        }

        @Override
        public boolean withdraw(long resource) {
            return published.withdraw(resource, disposer);
        }

        @Override
        public OptionalInt nameOf(long resource) {
            return published.nameOf(resource);
        }
    }
}
