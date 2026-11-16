package io.github.luasgraphicss.minemod.compatibility.interop.negotiation;

public final class ExternalSupport {
    private static final ExternalSupport UNSUPPORTED = new ExternalSupport(false, false, 0, 0, 0, 0);

    private final boolean shareable;
    private final boolean dedicatedAllocationRequired;
    private final long maxWidth;
    private final int maxHeight;
    private final int maxMipLevels;
    private final int sampleCounts;

    private ExternalSupport(
            boolean shareable,
            boolean dedicatedAllocationRequired,
            long maxWidth,
            int maxHeight,
            int maxMipLevels,
            int sampleCounts) {
        this.shareable = shareable;
        this.dedicatedAllocationRequired = dedicatedAllocationRequired;
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
        this.maxMipLevels = maxMipLevels;
        this.sampleCounts = sampleCounts;
    }

    public static ExternalSupport unsupported() {
        return UNSUPPORTED;
    }

    public static ExternalSupport shareable(
            boolean dedicatedAllocationRequired, long maxWidth, int maxHeight, int maxMipLevels, int sampleCounts) {
        return new ExternalSupport(true, dedicatedAllocationRequired, maxWidth, maxHeight, maxMipLevels, sampleCounts);
    }

    public boolean isShareable() {
        return shareable;
    }

    public boolean dedicatedAllocationRequired() {
        return dedicatedAllocationRequired;
    }

    public boolean fits(SharingRequest request) {
        return request.width() <= maxWidth && request.height() <= maxHeight && request.mipLevels() <= maxMipLevels;
    }

    public boolean supportsSamples(int samples) {
        return (sampleCounts & samples) == samples;
    }
}
