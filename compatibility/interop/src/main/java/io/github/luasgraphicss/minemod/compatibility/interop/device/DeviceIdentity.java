package io.github.luasgraphicss.minemod.compatibility.interop.device;

import java.util.Arrays;

public final class DeviceIdentity {
    public static final int UUID_BYTES = 16;

    private final byte[] deviceUuid;
    private final byte[] driverUuid;

    public DeviceIdentity(byte[] deviceUuid, byte[] driverUuid) {
        if (deviceUuid.length != UUID_BYTES || driverUuid.length != UUID_BYTES) {
            throw new IllegalArgumentException("device and driver UUIDs have " + UUID_BYTES + " bytes each");
        }
        this.deviceUuid = deviceUuid.clone();
        this.driverUuid = driverUuid.clone();
    }

    public boolean isSameGpuAs(DeviceIdentity other) {
        return Arrays.equals(deviceUuid, other.deviceUuid) && Arrays.equals(driverUuid, other.driverUuid);
    }
}
