# 1. Versioned C ABI with validated command streams

Status: accepted.

## Context

One native core has to serve Minecraft 1.12.2 on Java 8 with LWJGL 2 and Minecraft 1.21.1 on Java 21 with LWJGL 3. A renderer written against one LWJGL generation cannot load beside the other, and a C++ interface would bind every adapter to one compiler and standard library.

## Decision

Java reaches the native core only through the C ABI declared in `native/abi/include/minemod/abi`.

- Types have fixed widths and explicit reserved fields instead of padding. The implementation asserts the size of every struct that crosses.
- No C++ type, exception or Java reference crosses. Entry points are `noexcept` and turn `std::bad_alloc` and any other exception into a `minemod_result`.
- A client states the ABI version it was built for. While the major version is 0 the library accepts only its own minor version; from 1.0 on it accepts every minor version up to its own. `minemod_negotiate` also reports the capabilities it lacks, and `minemod_session_create` repeats both checks before it allocates.
- Work crosses in batches, never one call per vertex. A command stream is a little-endian buffer: a 16-byte header (magic, version, command count, total size) followed by 8-byte aligned records. A stream holds at most 16 MiB and 65,536 commands.
- The library validates the whole stream before it executes the first command: sizes against the buffer, alignment, opcodes, reserved bits, enumerations and offset overflow. A stream that fails validation changes nothing.
- `libminemod_jni` links the ABI statically and exports one symbol, `JNI_OnLoad`. Natives are bound with `RegisterNatives`, so a Java signature that drifted from the native one fails when the library loads instead of corrupting a call.

## Evidence

- `tests/fixtures/command-streams` holds 16 streams with the result each must produce. The C++ suite, the Java codec and the JNI bridge all check themselves against it.
- `NativeValidatorAgreementTest` feeds 200 generated streams and 6,000 mutations of them to the Java and native validators and requires identical reports, on Java 8, 16, 17 and 21.
- `allocation_failure_test.cpp` shows that an incompatible client is rejected before any allocation, and that a failed allocation returns `MINEMOD_RESULT_OUT_OF_MEMORY` and leaves the session usable.
- `c_consumer_test.c` compiles the headers as C17.
- The `linux-debug` preset runs the native suites under AddressSanitizer and UndefinedBehaviorSanitizer.

## Limits

The opcodes cover buffer creation, writes and destruction only. ABI 0.x promises no compatibility between minor versions. The Windows presets exist but have never been built.
