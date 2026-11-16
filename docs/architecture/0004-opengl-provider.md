# 4. Mesa/Zink as the OpenGL provider for mods

Status: proposed. `compatibility/provider` and `compatibility/dispatch` implement the pin, the identity check and the dispatch table and are tested against a simulated provider. No Mesa/Zink build has been loaded or measured.

## Context

Mods call desktop OpenGL directly, including fixed-function and old extensions. The native renderer speaks only Vulkan. Something has to turn those calls into Vulkan work on the same GPU.

## Proposal

Pin one Mesa release and use its Zink driver as the OpenGL implementation, behind a contract of our own so that the provider can be replaced. Mesa keeps OpenGL semantics and its shader compilation. The layer in this repository forwards calls, virtualizes object names and the default framebuffer, and hands resources across.

Rejected as a base: running the whole game on Zink (vanilla would stay bound to OpenGL semantics), writing an OpenGL implementation, ANGLE (its contract is OpenGL ES), and mixing the vendor's OpenGL with Vulkan (it would not show that mod calls reach Vulkan).

## Needed before acceptance

- A reproducible build of the pinned Mesa for Windows and Linux, with its licence obligations recorded.
- The Zink requirements of that release (`VP_ZINK_requirements.json`) checked against the reference machines.
- `GL_EXT_memory_object`, `GL_EXT_semaphore` and their `_fd` and `_win32` variants shown to work in that build, not only listed by it.
- A list of every Mesa internal the integration touches. None is allowed without a decision of its own.
