#include "minemod/abi/result.h"

const char* minemod_result_name(minemod_result result) noexcept {
    switch (result) {
        case MINEMOD_RESULT_OK:
            return "OK";
        case MINEMOD_RESULT_INVALID_ARGUMENT:
            return "INVALID_ARGUMENT";
        case MINEMOD_RESULT_INCOMPATIBLE_ABI:
            return "INCOMPATIBLE_ABI";
        case MINEMOD_RESULT_UNSUPPORTED_CAPABILITY:
            return "UNSUPPORTED_CAPABILITY";
        case MINEMOD_RESULT_TRUNCATED_STREAM:
            return "TRUNCATED_STREAM";
        case MINEMOD_RESULT_MALFORMED_STREAM:
            return "MALFORMED_STREAM";
        case MINEMOD_RESULT_UNKNOWN_OPCODE:
            return "UNKNOWN_OPCODE";
        case MINEMOD_RESULT_LENGTH_OUT_OF_BOUNDS:
            return "LENGTH_OUT_OF_BOUNDS";
        case MINEMOD_RESULT_MISALIGNED:
            return "MISALIGNED";
        case MINEMOD_RESULT_ARITHMETIC_OVERFLOW:
            return "ARITHMETIC_OVERFLOW";
        case MINEMOD_RESULT_INVALID_HANDLE:
            return "INVALID_HANDLE";
        case MINEMOD_RESULT_STALE_HANDLE:
            return "STALE_HANDLE";
        case MINEMOD_RESULT_LIMIT_EXCEEDED:
            return "LIMIT_EXCEEDED";
        case MINEMOD_RESULT_OUT_OF_MEMORY:
            return "OUT_OF_MEMORY";
        case MINEMOD_RESULT_INTERNAL_ERROR:
            return "INTERNAL_ERROR";
        default:
            return "UNKNOWN_RESULT";
    }
}
