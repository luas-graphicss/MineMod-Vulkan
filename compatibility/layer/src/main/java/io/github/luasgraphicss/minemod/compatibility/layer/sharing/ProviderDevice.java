package io.github.luasgraphicss.minemod.compatibility.layer.sharing;

import io.github.luasgraphicss.minemod.compatibility.interop.device.DeviceIdentity;
import io.github.luasgraphicss.minemod.compatibility.layer.memory.NativeMemory;
import io.github.luasgraphicss.minemod.compatibility.translation.provider.ProviderCalls;

public final class ProviderDevice {
    private static final int GL_DEVICE_UUID_EXT = 0x9597;
    private static final int GL_DRIVER_UUID_EXT = 0x9598;
    private static final int FIRST_DEVICE = 0;

    private ProviderDevice() {
    }

    public static DeviceIdentity identify(ProviderCalls provider, NativeMemory memory) {
        long uuid = memory.allocate(DeviceIdentity.UUID_BYTES);
        try {
            provider.invoke("glGetUnsignedBytei_vEXT", new long[] {GL_DEVICE_UUID_EXT, FIRST_DEVICE, uuid});
            byte[] device = bytesAt(memory, uuid);
            provider.invoke("glGetUnsignedBytevEXT", new long[] {GL_DRIVER_UUID_EXT, uuid});
            return new DeviceIdentity(device, bytesAt(memory, uuid));
        } finally {
            memory.free(uuid);
        }
    }

    private static byte[] bytesAt(NativeMemory memory, long address) {
        byte[] bytes = new byte[DeviceIdentity.UUID_BYTES];
        for (int index = 0; index < bytes.length; index++) {
            bytes[index] = memory.readByte(address + index);
        }
        return bytes;
    }
}
