# 4. Mesa/Zink as the OpenGL provider for mods

Status: proposed. `compatibility/provider` and `compatibility/dispatch` implement the pin, the identity check and the dispatch table. `native/gl-provider` loads a Mesa/Zink from a private directory on Linux, and the layer draws through the Mesa 24.2.6 of the development machine on lavapipe. No Mesa build is pinned or built reproducibly, and nothing has been measured.

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

## Shown so far

On Linux, with the Mesa a distribution ships and lavapipe underneath. `docs/development/compatibility-layer.md` names the tests.

- A bundle is loaded from a directory of its own after the digest of every library was checked, and every function the layer exposes resolves inside the image of that bundle.
- An OpenGL 4.5 context in the core profile and one in the compatibility profile serve the calls the tests make through LWJGL 3 and LWJGL 2.
- `GL_EXT_memory_object_fd` imports memory that another Vulkan device exported, for a color and a depth/stencil attachment.

Not shown: `GL_EXT_semaphore`, which Zink does not offer on lavapipe, the `_win32` variants, and any hardware GPU. The integration calls only exported EGL and OpenGL entry points and puts the driver settings a bundle declares into the environment the provider reads, which for the test bundle are `MESA_LOADER_DRIVER_OVERRIDE`, `GALLIUM_DRIVER` and `LIBGL_ALWAYS_SOFTWARE`. It touches no Mesa internal.
