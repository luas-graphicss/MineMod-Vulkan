# The OpenGL compatibility layer

Mods reach OpenGL through LWJGL. The modules under `compatibility/` decide what happens to each of those calls and keep the provider's drawing in order with the native renderer inside one frame. They are Java 8 libraries. Only `compatibility/lwjgl2` and `compatibility/lwjgl3` compile against LWJGL, and none depends on Minecraft or a mod loader.

The layer is checked in two ways. Against a simulated provider that records the calls it receives, every test runs without a GPU. Against Mesa's Zink, on Linux, the tests load the Mesa installed on the machine from a private directory and draw through it on lavapipe. No pinned Mesa build exists yet, and nothing on this page has run on a hardware GPU, on Windows or inside the game.

## Check it

Against the simulated provider:

```sh
sh gradlew :tools:verification:run --args="check --profile compatibility"
sh gradlew :tools:verification:run --args="scenario mixed-render-state"
sh gradlew :tools:verification:run --args="scenario legacy-gl-contract"
sh gradlew :tools:verification:run --args="scenario shared-resource-lifecycle"
sh gradlew :tools:verification:run --args="scenario synchronous-operations"
sh gradlew :tools:verification:run --args="compare --suite gl-differential"
sh gradlew :tools:verification:run --args="capabilities --context core"
```

The profile runs the catalog check, the four scenarios and the differential suite. CI runs it after `gradlew check`.

Against Mesa/Zink:

```sh
cmake --preset linux-client
cmake --build --preset linux-scene-host
sh gradlew :compatibility:runtime:check :compatibility:lwjgl2:check :compatibility:lwjgl3:check \
    -Pminemod.systemProvider=/usr/lib64 \
    -Pminemod.sceneHost="$PWD/build/linux-client/jni/libminemod_scene_host_jni.so"
```

`minemod.systemProvider` names the directory that holds `libEGL.so.1`, `libEGL_mesa.so.0` and `libgallium-*.so`: `/usr/lib64` on Fedora and Amazon Linux, `/usr/lib/x86_64-linux-gnu` on Debian and Ubuntu. Mesa ships that driver library since 24.2. The tests copy the libraries into a directory of their own, pin the digests of the copies and load them from there with Zink on a software device.

`minemod.sceneHost` is the library of a small Vulkan device that stands in for the renderer: it owns the scene attachments, exports their memory and draws its own passes on them.

Without the first property every test of this group is skipped, and without the second only the shared-scene tests are, so `gradlew check` passes on a machine without Mesa. The CI job `Compatibility layer on Mesa/Zink` sets both. The tests run on Java 8, 16, 17 and 21, except those of LWJGL 2, which run on Java 8, the runtime of the releases that ship it.

`capabilities` prints one row per call family and one column per class of support, then the operations a frame reported instead of answering:

```text
core context:
  family                           supported  waiting for a bridge  outside the contract          not verified
  immediate-mode                           0                     0                   214                     0
  framebuffer                             23                     0                     0                     0
  total                                  387                     0                   379                     0
reported while a frame runs:
  waiting for a bridge: transform feedback across native draws
  waiting for a bridge: query result across native draws
  outside the contract: glDrawBuffer on the front buffer
```

The table is static: it is what the dispatch table resolves when the provider exports every function the catalog describes. A bridge is never needed for a function as such, only for what a frame does with it, which is why that column is filled by the lines below the table. A function the catalog does not describe is withheld as not verified.

## Modules

| Module | What it owns |
| --- | --- |
| `compatibility/contracts` | Object kinds and their sharing scope, the four classes of support, and the report of an operation that was not served: what it was, the resource it touched and the mod that most likely made the call. |
| `compatibility/provider` | The pinned provider artifact and its SHA-256, loading it from a private directory, and the diagnosis that accepts only the pinned Mesa/Zink build with the required OpenGL version and extensions. |
| `compatibility/dispatch` | The call catalog (`gl-calls.txt`, `gl-extensions.txt`), the dispatch table that sends a function to the provider, to the layer or nowhere, the check that every provider address lies inside the provider's own image, and the installation before LWJGL 2 or LWJGL 3 resolves its pointers. |
| `compatibility/contexts` | Context lifecycle, share groups and the thread a context is current on. |
| `compatibility/resources` | Name tables per context and share group, the virtual default framebuffer, aliases of vanilla textures and their transactional redefinition. |
| `compatibility/state` | Reconciliation between wrapper state and direct OpenGL state, the `glGetError` order, and the submission checkpoints that hand the scene attachments from one side to the other. |
| `compatibility/translation` | What the layer does with a call it intercepts: name translation, framebuffer calls, display lists and other open legacy constructs, queries and transform feedback, texture storage of aliases, and the sections where native passes and callbacks alternate. |
| `compatibility/interop` | Negotiation of shareable images, ownership of the exported handles, the semaphore order of a handoff and the pool that recreates shared resources on reload. |
| `jvm/compatibility-spi` | The interface a bridge implements. It exposes scene attachments and a scoped context, never a queue or the swapchain. |
| `compatibility/bridges` | Discovery of bridges, the SPI version check and the session that scopes what a bridge may touch. |
| `native/gl-provider` | The native side of the provider, on Linux. It loads the libraries of a bundle from its directory by file name, dependencies first, follows no symbolic link and records the address ranges of what it loaded. It opens a surfaceless EGL display, creates contexts with a version, a profile and a share group, and invokes any function of the catalog by index, with integer and floating-point arguments. A table generated from the Khronos registry and the catalog gives each function a mod may call a thunk that hands its arguments to whatever intercepts. Its JNI library also replaces the function lookup of LWJGL 2. |
| `compatibility/natives` | The Java face of that library: the opener that checks every digest before a library is loaded, contexts, provider calls by name, native memory and the gate through which a thunk reaches the layer. |
| `compatibility/layer` | What intercepts. For one context it composes the modules above: virtual names, the scene as framebuffer zero, state queries answered from the tables, wrapper state applied on entry to a callback and adopted or restored on exit, textures the renderer publishes and the storage a mod gets when it redefines one, the extensions that are exposed and the import of shared memory into the provider. |
| `compatibility/runtime` | A session over one provider bundle: the digest check, the load, the diagnosis of the context the provider really created, the dispatch table checked against the provider's own image, contexts with their share groups and threads, and the installation into LWJGL. |
| `compatibility/lwjgl3`, `compatibility/lwjgl2` | The installation for each generation. LWJGL 3 receives a function provider before it creates capabilities, and LWJGL 2 has its lookup replaced before it loads an OpenGL library. A second installation is refused. |
| `native/interop` | The renderer's side of sharing, on Linux: device identity, images, buffers and semaphores that can be exported, and the handoff that moves the scene attachments out of the renderer's queue family and back with a semaphore in each direction. `native/interop/harness` is the stand-in renderer of the tests. |

## What the scenarios cover

`mixed-render-state` plays a frame in which native passes, wrapper state and direct OpenGL alternate, checks the order of every event against a fixed script, then generates 10,000 frames from a recorded seed and checks that the wrappers and the provider agree after each one.

`legacy-gl-contract` resolves the fixed-function entry points in a compatibility context, checks that a core context withholds them, and compares the provider calls of an immediate-mode and display-list callback with a reference that calls the provider directly.

`shared-resource-lifecycle` runs 1,000 create, reload and destroy cycles for file descriptors and for NT handles with imports that fail part of the way, rejects a different GPU and a provider without the semaphore extensions, and destroys sharing contexts out of order.

`synchronous-operations` compares a partial blit and two readbacks with pack alignments 1 and 4 against the reference, passes a failed compile, a failed link and a pending query through unchanged, and checks that a query or transform feedback that spans a native pass is reported and never answered from the provider.

`gl-differential` plays 1,000 generated scripts through the layer and directly against the provider and compares the calls and the errors.

## What the tests on Mesa/Zink cover

Each test draws through the layer and reads pixels, names, logs or errors back from the provider.

`ProviderSessionTest` accepts the staged bundle as Zink and resolves every exposed function inside the provider's own image, refuses an entry library or a dependency whose bytes changed before loading it, and rejects a software device by name unless the profile allows one.

`LayerOnZinkTest` covers the scene as framebuffer zero, a framebuffer of the mod with its own pixels and names, a partial blit, a readback under the caller's pack alignment, separate read and draw bindings, the order of `glGetError` across the provider and the layer, a shader that does not compile and a program that does not link, samplers, a fence, a draw from a vertex buffer counted by a query, a query that spans a native pass, the extension list, and wrapper state on entry and exit of a callback.

`MixedFrameOnZinkTest` checks that a callback between native passes flushes once for each change of owner, that a frame without provider work flushes nothing, and that transform feedback across a native pass is reported.

`SharedTexturesOnZinkTest` publishes a texture under the name a mod sees, moves the mod to storage of its own when it redefines that texture, and keeps an import or a deleted texture alive until the frame that used it is presented.

`ContextsOnZinkTest` shares a texture and not a framebuffer between two contexts, refuses a context that is current on another thread, closes a share group out of order, and creates and closes contexts 100 times without leaving a context, an object or a descriptor behind.

`SharedSceneOnZinkTest` imports the memory of the stand-in renderer's color and depth/stencil attachments into Zink through `GL_EXT_memory_object_fd`. The renderer reads what a mod drew over its pass, a mod reads the pass the renderer drew between two callbacks, and depth the renderer wrote hides what a mod draws behind it. It also negotiates the sharing on one GPU, and shares and releases attachments 1,000 times without leaving an import or a descriptor behind.

`Lwjgl3OnZinkTest` and `Lwjgl2OnZinkTest` make the same calls through the real bindings. LWJGL sees only the exposed extensions, receives virtual names and draws into the scene. On LWJGL 2 a compatibility context runs immediate mode on the matrix stack, a display list and fixed-function texturing, and the pixels equal those of the same calls made without the binding.

`ctest` covers the native side: `gl_provider` loads a provider built for the test, and `interop_device` runs the handoff 200 times on lavapipe with the validation layers on.

## Not verified

- No Mesa build is pinned. The tests accept the Mesa of the machine, 24.2.6 on the machine this was written on, and the pin of `compatibility/provider` is filled from the staged copy.
- Everything ran on lavapipe. No hardware GPU has drawn through the layer, so two drivers on one GPU, vendor behaviour and performance are untested.
- The semaphore handoff with the provider has not run. lavapipe exports no semaphore, so Zink offers neither `GL_EXT_semaphore` nor `GL_EXT_semaphore_fd` on it. The shared-scene tests order the two devices by finishing each side before the other draws. `ProviderHandoff` and `SplitSharingBackend`, which wait and signal through `GL_EXT_semaphore`, have no test, and the handoff of `native/interop` is tested with both semaphores on one device.
- The renderer does not export its scene attachments, and no game adapter opens a compatibility session. The stand-in device takes the renderer's place in every test, and `RendererHandoff` and `RendererResources` have no implementation.
- Stencil of a shared attachment and multisampled attachments are not read by any test.
- No bridge for a real mod exists, and no mod has run on top of the layer.
- Windows has no provider loader and no handle sharing. It is covered only as a handle type in the lifecycle scenario.
