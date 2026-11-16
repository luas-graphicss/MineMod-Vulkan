# 7. Failures are values at the boundary, never a silent fallback

Status: accepted for the ABI, the bridge and the harness. Device loss and recovery are open.

## Context

The core runs inside someone else's game process and is fed input it does not control. A wrong answer that looks right (a stale texture, a corrupted frame, a software renderer under a Vulkan name) is worse than a refusal.

## Decision

- Every ABI call returns a `minemod_result`, and `minemod_result_name` names it. Nothing is reported through a global or a callback.
- A stream failure comes with a report: the commands completed, the index of the failing command and its byte offset.
- A validation failure applies nothing. A failure during execution, such as a stale handle in the third command, keeps the commands already executed, stops, and is counted as an aborted submission.
- On the JVM the outcome of a stream is data (`StreamReport`). Misuse throws: `AbiException` carries the result of a refused negotiation, session or read, `IllegalStateException` marks a closed session and `IllegalArgumentException` a heap buffer that cannot cross.
- A capability is advertised only when it is implemented. Requiring one the library lacks fails negotiation with the missing bits.
- An unusable system is refused with its reason: the missing instance extension, the absent validation layer, or each device with the requirement it failed. There is no fallback renderer.

## Evidence

- `command_stream_validation_test.cpp`, the corpus and the mutation tests cover truncated, oversized, misaligned and overflowing streams and unknown opcodes.
- `NativeSessionTest` checks the report of an aborted submission and the exceptions for closed sessions and heap buffers.
- `device_requirements_test.cpp` requires the rejection to name the missing extension. Run without a usable driver, `minemod_harness` prints `the Vulkan installation lacks the instance extension VK_KHR_surface` and exits with status 1.

## Open

Device loss, an out-of-date swapchain, recovery after a failed reload and the rules for configuration and caches have no implementation to test yet.
