package io.github.luasgraphicss.minemod.bridge.sharing;

public final class AttachmentDescription {
    private final long allocationBytes;
    private final int vulkanFormat;
    private final int vulkanLayout;
    private final int width;
    private final int height;

    public AttachmentDescription(long allocationBytes, int vulkanFormat, int vulkanLayout, int width, int height) {
        if (allocationBytes < 1 || width < 1 || height < 1) {
            throw new IllegalArgumentException(
                    "a shared attachment needs memory and an area: " + allocationBytes + " bytes for " + width + "x"
                            + height);
        }
        this.allocationBytes = allocationBytes;
        this.vulkanFormat = vulkanFormat;
        this.vulkanLayout = vulkanLayout;
        this.width = width;
        this.height = height;
    }

    public long allocationBytes() {
        return allocationBytes;
    }

    public int vulkanFormat() {
        return vulkanFormat;
    }

    public int vulkanLayout() {
        return vulkanLayout;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    @Override
    public String toString() {
        return width + "x" + height + " of VkFormat " + vulkanFormat + " handed over in VkImageLayout " + vulkanLayout
                + " (" + allocationBytes + " bytes)";
    }
}
