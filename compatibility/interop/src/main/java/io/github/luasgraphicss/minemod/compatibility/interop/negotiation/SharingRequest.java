package io.github.luasgraphicss.minemod.compatibility.interop.negotiation;

import java.util.Objects;

public final class SharingRequest {
    private static final int NO_FORMAT = 0;

    private final ShareableResource resource;
    private final int format;
    private final long width;
    private final int height;
    private final int mipLevels;
    private final int samples;

    private SharingRequest(
            ShareableResource resource, int format, long width, int height, int mipLevels, int samples) {
        if (width < 1 || height < 1 || mipLevels < 1 || samples < 1) {
            throw new IllegalArgumentException("a shared resource has a positive size, mip count and sample count");
        }
        this.resource = Objects.requireNonNull(resource, "resource");
        this.format = format;
        this.width = width;
        this.height = height;
        this.mipLevels = mipLevels;
        this.samples = samples;
    }

    public static SharingRequest image(
            ShareableResource resource, int format, int width, int height, int mipLevels, int samples) {
        if (resource == ShareableResource.BUFFER) {
            throw new IllegalArgumentException("a buffer is requested by its size");
        }
        return new SharingRequest(resource, format, width, height, mipLevels, samples);
    }

    public static SharingRequest buffer(long sizeBytes) {
        return new SharingRequest(ShareableResource.BUFFER, NO_FORMAT, sizeBytes, 1, 1, 1);
    }

    public ShareableResource resource() {
        return resource;
    }

    public int format() {
        return format;
    }

    public long width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int mipLevels() {
        return mipLevels;
    }

    public int samples() {
        return samples;
    }

    @Override
    public String toString() {
        if (resource == ShareableResource.BUFFER) {
            return "BUFFER of " + width + " bytes";
        }
        return resource + " format " + format + " " + width + "x" + height + ", " + mipLevels + " mip level(s), "
                + samples + " sample(s)";
    }
}
