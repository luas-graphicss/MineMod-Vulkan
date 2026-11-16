package io.github.luasgraphicss.minemod.compatibility.layer.sharing;

public enum SharedFormat {
    RGBA8(37, 0x8058),
    SRGB8_ALPHA8(43, 0x8C43),
    RGBA16F(97, 0x881A),
    DEPTH24_STENCIL8(129, 0x88F0),
    DEPTH32F_STENCIL8(130, 0x8CAD);

    private final int vulkanFormat;
    private final int internalFormat;

    SharedFormat(int vulkanFormat, int internalFormat) {
        this.vulkanFormat = vulkanFormat;
        this.internalFormat = internalFormat;
    }

    public static SharedFormat ofVulkanFormat(int vulkanFormat) {
        for (SharedFormat format : values()) {
            if (format.vulkanFormat == vulkanFormat) {
                return format;
            }
        }
        throw new IllegalArgumentException("no OpenGL internal format is paired with Vulkan format " + vulkanFormat);
    }

    public int internalFormat() {
        return internalFormat;
    }
}
