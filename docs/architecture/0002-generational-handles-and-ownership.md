# 2. Generational handles and explicit ownership

Status: accepted for session resources. Retirement of GPU resources is open.

## Context

Resources are named on the JVM and live in native memory. A reload or a late callback can present a handle whose resource is gone; that handle must never reach whatever occupies its slot now. The JVM may also recycle a buffer as soon as a call returns.

## Decision

- A handle is 64 bits: resource kind (8), generation (24) and slot index (32). Generation 0 is never valid, so a zeroed handle names nothing.
- The client allocates handles. `HandleAllocator` hands out a slot with its next generation and retires the slot for good once the generation would wrap.
- The session accepts a creation only with a generation newer than the last one it saw in that slot. A destroyed or superseded generation yields `STALE_HANDLE`; a handle that was never created yields `INVALID_HANDLE`.
- The session copies every byte a stream writes. The caller owns its direct buffer again when `submit` returns, and the JNI layer keeps no reference to it.
- A session has budgets for the number of buffers and for their total bytes, fixed at creation and capped by the library at 2^20 buffers and 1 GiB. Exceeding one yields `LIMIT_EXCEEDED`.
- A session counts accepted, rejected and aborted submissions, executed commands, created, destroyed and live buffers, and live bytes.
- Native code holds resources in RAII owners such as `DeviceHandle`, `Buffer` and `Image`. The only raw pointer that changes owner is the session that `minemod_session_create` hands out and `minemod_session_destroy` takes back.

## Evidence

- `session_test.cpp` and `NativeSessionTest` cover stale and never-created handles, generation reuse, budgets and counters.
- `NativeSessionTest.keepsWrittenBytesAfterTheSubmittedBufferIsRecycled` overwrites the submitted direct buffer and reads the original bytes back.
- `HandleAllocatorTest` covers slot reuse and retirement.
- The harness tests run with the validation layers and require zero messages, which includes objects left alive when the device is destroyed.

## Open

Destruction has to wait for every consumer, including the OpenGL provider, and not only for the renderer's own fence. Nothing implements that yet. Counters for VRAM, OS handles and OpenGL objects arrive with the resources they count.
