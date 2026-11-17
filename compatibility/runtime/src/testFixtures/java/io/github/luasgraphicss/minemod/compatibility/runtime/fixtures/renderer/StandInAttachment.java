package io.github.luasgraphicss.minemod.compatibility.runtime.fixtures.renderer;

import io.github.luasgraphicss.minemod.compatibility.interop.negotiation.SharingRequest;

public final class StandInAttachment {
    private static final int RED_SHIFT = 24;
    private static final int GREEN_SHIFT = 16;
    private static final int BLUE_SHIFT = 8;
    private static final int CHANNEL_MASK = 0xFF;

    private final long host;
    private final int attachment;
    private final SharingRequest request;

    StandInAttachment(long host, int attachment, SharingRequest request) {
        this.host = host;
        this.attachment = attachment;
        this.request = request;
    }

    public SharingRequest request() {
        return request;
    }

    public long allocationSize() {
        return StandInNatives.allocationSize(host, attachment);
    }

    public long exportMemory() {
        return StandInNatives.exportMemory(host, attachment);
    }

    public void drawColor(int x, int y, int width, int height, int[] color) {
        int packed = (color[0] << RED_SHIFT) | (color[1] << GREEN_SHIFT) | (color[2] << BLUE_SHIFT) | color[3];
        StandInNatives.clearColor(host, attachment, x, y, width, height, packed);
    }

    public void drawDepth(float depth) {
        StandInNatives.clearDepth(host, attachment, (int) request.width(), request.height(), depth);
    }

    public int[] colorAt(int x, int y) {
        int packed = StandInNatives.colorAt(host, attachment, x, y);
        return new int[] {
            (packed >>> RED_SHIFT) & CHANNEL_MASK,
            (packed >>> GREEN_SHIFT) & CHANNEL_MASK,
            (packed >>> BLUE_SHIFT) & CHANNEL_MASK,
            packed & CHANNEL_MASK
        };
    }

    public void destroy() {
        StandInNatives.destroyAttachment(host, attachment);
    }
}
