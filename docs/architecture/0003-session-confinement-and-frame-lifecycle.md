# 3. Session confinement and frame lifecycle

Status: session confinement accepted. The frame lifecycle is proposed.

## Context

The client's main thread owns game state and mod callbacks. The native core will add workers and a presentation loop. JNI has a rule of its own: a `JNIEnv` belongs to one thread.

## Decision

Accepted:

- A native session is not thread-safe and has one owner at a time. `NativeSession` serializes its calls on its own monitor and clears the native address on `close`, so closing while another thread submits ends that thread with `IllegalStateException` instead of freeing memory in use.
- The JNI layer stores no `JNIEnv`, no local reference and no buffer address between calls.
- A binary semaphore handed to presentation stays in use until the queue drains. `Swapchain` waits for the device to go idle only when it is destroyed.

Proposed, to be confirmed by the frame loop:

- Two frames in flight by default. The number of swapchain images is a separate number.
- One owner per Vulkan queue; command pools and arenas owned per thread and per frame.
- Bounded queues with backpressure between the client thread and the workers, and cancellation by generation.
- No `vkDeviceWaitIdle` per frame, upload or callback.
- Separate paths for `VK_ERROR_OUT_OF_DATE_KHR`, `VK_SUBOPTIMAL_KHR` and `VK_ERROR_DEVICE_LOST`.

## Evidence

`NativeSessionShutdownTest` closes a session while another thread submits, and cancels a submitting task, on the four Java runtimes. The harness draws and waits one submission at a time, so it says nothing about frames in flight.
