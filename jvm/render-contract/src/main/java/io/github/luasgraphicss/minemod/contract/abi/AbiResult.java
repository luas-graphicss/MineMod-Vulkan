package io.github.luasgraphicss.minemod.contract.abi;

public enum AbiResult {
    OK(0),
    INVALID_ARGUMENT(1),
    INCOMPATIBLE_ABI(2),
    UNSUPPORTED_CAPABILITY(3),
    TRUNCATED_STREAM(4),
    MALFORMED_STREAM(5),
    UNKNOWN_OPCODE(6),
    LENGTH_OUT_OF_BOUNDS(7),
    MISALIGNED(8),
    ARITHMETIC_OVERFLOW(9),
    INVALID_HANDLE(10),
    STALE_HANDLE(11),
    LIMIT_EXCEEDED(12),
    OUT_OF_MEMORY(13),
    INTERNAL_ERROR(14);

    private static final AbiResult[] BY_CODE = values();

    private final int code;

    AbiResult(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static AbiResult fromCode(int code) {
        if (code < 0 || code >= BY_CODE.length) {
            throw new IllegalArgumentException("unknown ABI result code: " + code);
        }
        return BY_CODE[code];
    }
}
