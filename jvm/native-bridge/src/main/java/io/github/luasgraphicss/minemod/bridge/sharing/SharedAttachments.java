package io.github.luasgraphicss.minemod.bridge.sharing;

public final class SharedAttachments {
    public static final int UUID_BYTES = 16;

    private final byte[] deviceUuid;
    private final byte[] driverUuid;
    private final AttachmentDescription color;
    private final AttachmentDescription depthStencil;
    private final boolean sharesSemaphores;
    private final long generation;

    public SharedAttachments(
            byte[] deviceUuid,
            byte[] driverUuid,
            AttachmentDescription color,
            AttachmentDescription depthStencil,
            boolean sharesSemaphores,
            long generation) {
        if (deviceUuid.length != UUID_BYTES || driverUuid.length != UUID_BYTES) {
            throw new IllegalArgumentException(
                    "device and driver UUIDs have " + UUID_BYTES + " bytes, found " + deviceUuid.length + " and "
                            + driverUuid.length);
        }
        this.deviceUuid = deviceUuid.clone();
        this.driverUuid = driverUuid.clone();
        this.color = color;
        this.depthStencil = depthStencil;
        this.sharesSemaphores = sharesSemaphores;
        this.generation = generation;
    }

    public byte[] deviceUuid() {
        return deviceUuid.clone();
    }

    public byte[] driverUuid() {
        return driverUuid.clone();
    }

    public AttachmentDescription attachment(SharedAttachment which) {
        return which == SharedAttachment.COLOR ? color : depthStencil;
    }

    public boolean sharesSemaphores() {
        return sharesSemaphores;
    }

    public long generation() {
        return generation;
    }
}
