package io.github.luasgraphicss.minemod.bridge;

import io.github.luasgraphicss.minemod.contract.abi.AbiException;
import io.github.luasgraphicss.minemod.contract.abi.AbiNegotiation;
import io.github.luasgraphicss.minemod.contract.abi.AbiResult;
import io.github.luasgraphicss.minemod.contract.abi.AbiVersion;
import io.github.luasgraphicss.minemod.contract.capability.Capability;
import io.github.luasgraphicss.minemod.contract.session.SessionLimits;
import io.github.luasgraphicss.minemod.contract.stream.StreamReport;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Set;

public final class NativeBridge {
    private static final int LIBRARY_MAJOR = 0;
    private static final int LIBRARY_MINOR = 1;
    private static final int SUPPORTED_CAPABILITIES = 2;
    private static final int MISSING_CAPABILITIES = 3;
    private static final int MAX_STREAM_BYTES = 4;
    private static final int MAX_STREAM_COMMANDS = 5;
    private static final int NEGOTIATION_FIELDS = 6;

    private final AbiNegotiation negotiation;
    private final long requiredCapabilities;

    private NativeBridge(AbiNegotiation negotiation, long requiredCapabilities) {
        this.negotiation = negotiation;
        this.requiredCapabilities = requiredCapabilities;
    }

    public static NativeBridge load(Path library, Set<Capability> requiredCapabilities) {
        System.load(library.toAbsolutePath().toString());
        return negotiate(AbiVersion.CURRENT, requiredCapabilities);
    }

    static NativeBridge negotiate(AbiVersion clientVersion, Set<Capability> requiredCapabilities) {
        long requiredMask = Capability.maskOf(requiredCapabilities);
        long[] fields = new long[NEGOTIATION_FIELDS];
        AbiResult result = AbiResult.fromCode(
                nativeNegotiate(clientVersion.major(), clientVersion.minor(), requiredMask, fields));
        AbiVersion libraryVersion = new AbiVersion((int) fields[LIBRARY_MAJOR], (int) fields[LIBRARY_MINOR]);
        if (result != AbiResult.OK) {
            throw new AbiException(
                    result,
                    "negotiating ABI " + clientVersion + " with native library " + libraryVersion
                            + " (missing capabilities 0x" + Long.toHexString(fields[MISSING_CAPABILITIES]) + ")");
        }
        AbiNegotiation negotiation = new AbiNegotiation(
                libraryVersion, fields[SUPPORTED_CAPABILITIES], fields[MAX_STREAM_BYTES], fields[MAX_STREAM_COMMANDS]);
        return new NativeBridge(negotiation, requiredMask);
    }

    public AbiNegotiation negotiation() {
        return negotiation;
    }

    public StreamReport validate(ByteBuffer stream) {
        DirectBuffers.requireDirect(stream, "stream");
        long[] fields = new long[StreamReportFields.COUNT];
        int resultCode = nativeValidateStream(stream, stream.position(), stream.remaining(), fields);
        return StreamReportFields.decode(resultCode, fields);
    }

    public NativeSession openSession(SessionLimits limits) {
        return NativeSession.open(AbiVersion.CURRENT, requiredCapabilities, limits);
    }

    private static native int nativeNegotiate(
            int clientMajor, int clientMinor, long requiredCapabilities, long[] negotiationFields);

    private static native int nativeValidateStream(ByteBuffer stream, int offset, int length, long[] reportFields);
}
