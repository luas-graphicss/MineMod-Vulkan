# 5. Sharing images between the renderer and the OpenGL provider

Status: proposed. `compatibility/interop` implements the negotiation, the handle ownership and the semaphore order, and `native/interop` exports the memory and hands the attachments over on the renderer's side. On lavapipe, Zink imported memory that another Vulkan device exported, and both sides drew on it in turn, ordered by finishing one side before the other drew. The semaphore round trip between the two devices has not run, and nothing has run on a hardware GPU or on Windows.

## Context

Zink manages its own Vulkan device. A `VkImage` of the renderer means nothing to it, and its queues are not ours to drive.

## Proposal

Share through external memory and external binary semaphores on the same physical device.

- Compare device and driver UUIDs before sharing anything, and reject a different GPU.
- Query `vkGetPhysicalDeviceImageFormatProperties2` with external memory information for every format, usage and sample count that is shared. Colour support says nothing about depth/stencil or MSAA.
- Use dedicated allocations where the driver requires them. Sub-allocated memory is not importable by default.
- Hand over with a binary semaphore in each direction. The renderer finishes the work the callback must see, releases the attachments and signals. The provider waits, runs the OpenGL calls in their original order, flushes and signals back. The renderer waits and reacquires. Each frame is presented once.
- Linux passes file descriptors, whose ownership moves on a successful import. Windows passes NT handles, whose ownership does not. Each side closes exactly what it still owns.
- No frame goes through the CPU. A profile that cannot share is rejected by name; there is no hidden software or OpenGL fallback.

## Needed before acceptance

- A native draw, an OpenGL draw through Zink and a second native draw on one image, with colour and depth/stencil, on one Windows and one Linux machine with a real GPU.
- Export, import, resize, destruction and partial failure repeated 10,000 times, with file descriptor or handle counts taken before and after.
- Rejection tests for a different GPU, a missing extension and a format that cannot be exported.
