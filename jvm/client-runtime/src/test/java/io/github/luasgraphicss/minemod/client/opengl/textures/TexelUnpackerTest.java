package io.github.luasgraphicss.minemod.client.opengl.textures;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

class TexelUnpackerTest {
    private static final int UNPACK_ROW_LENGTH = 0x0CF2;
    private static final int UNPACK_SKIP_ROWS = 0x0CF3;
    private static final int UNPACK_SKIP_PIXELS = 0x0CF4;
    private static final int UNPACK_ALIGNMENT = 0x0CF5;

    private final PixelStore store = new PixelStore();

    @Test
    void copiesTightRgbaRows() {
        byte[] texels = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};

        assertArrayEquals(texels, unpack(texels, TexelFormat.RGBA, 2, 2));
    }

    @Test
    void readsAnAreaInsideAWiderImage() {
        store.set(UNPACK_ROW_LENGTH, 3);
        store.set(UNPACK_SKIP_PIXELS, 1);
        store.set(UNPACK_SKIP_ROWS, 1);
        byte[] image = new byte[3 * 3 * 4];
        for (int index = 0; index < image.length; index++) {
            image[index] = (byte) (index / 4);
        }

        byte[] area = unpack(image, TexelFormat.RGBA, 2, 2);

        assertArrayEquals(new byte[] {4, 4, 4, 4, 5, 5, 5, 5, 7, 7, 7, 7, 8, 8, 8, 8}, area);
    }

    @Test
    void spreadsASingleChannelOverEveryChannel() {
        store.set(UNPACK_ALIGNMENT, 1);

        assertArrayEquals(new byte[] {9, 9, 9, 9, 7, 7, 7, 7}, unpack(new byte[] {9, 7}, TexelFormat.RED, 2, 1));
    }

    @Test
    void readsOpaqueTexelsFromThreeChannels() {
        store.set(UNPACK_ALIGNMENT, 1);

        assertArrayEquals(new byte[] {1, 2, 3, -1}, unpack(new byte[] {1, 2, 3}, TexelFormat.RGB, 1, 1));
    }

    @Test
    void readsLuminanceAndAlphaFromTwoChannels() {
        store.set(UNPACK_ALIGNMENT, 1);

        assertArrayEquals(new byte[] {5, 5, 5, 8}, unpack(new byte[] {5, 8}, TexelFormat.RED_GREEN, 1, 1));
    }

    @Test
    void skipsThePaddingThatAlignsEachRow() {
        byte[] padded = {1, 2, 3, 0, 4, 5, 6, 0};

        byte[] area = unpack(padded, TexelFormat.RED, 3, 2);

        assertArrayEquals(
                new byte[] {1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 6, 6, 6, 6}, area);
    }

    @Test
    void keepsTheAlignmentWhenTheGameAsksForOneThatDoesNotExist() {
        store.set(UNPACK_ALIGNMENT, 3);
        byte[] padded = {1, 2, 3, 0, 4, 5, 6, 0};

        assertArrayEquals(new byte[] {4, 4, 4, 4}, unpackFirstTexelOfTheSecondRow(padded));
    }

    @Test
    void refusesASourceShorterThanTheArea() {
        assertThrows(
                IllegalArgumentException.class,
                () -> TexelUnpacker.toRgba(ByteBuffer.allocate(15), TexelFormat.RGBA, store, 2, 2));
    }

    private byte[] unpack(byte[] source, TexelFormat format, int width, int height) {
        return TexelUnpacker.toRgba(ByteBuffer.wrap(source), format, store, width, height).array();
    }

    private byte[] unpackFirstTexelOfTheSecondRow(byte[] padded) {
        store.set(UNPACK_SKIP_ROWS, 1);
        return TexelUnpacker.toRgba(ByteBuffer.wrap(padded), TexelFormat.RED, store, 1, 1).array();
    }
}
