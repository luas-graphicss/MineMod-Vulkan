# The OpenGL compatibility layer

Mods reach OpenGL through LWJGL. The modules under `compatibility/` decide what happens to each of those calls and keep the provider's drawing in order with the native renderer inside one frame. They are Java 8 libraries with no dependency on LWJGL, Minecraft or a mod loader, so every test runs without a GPU.

No Mesa/Zink build has been loaded yet. The provider behind every result on this page is a simulation that records the calls it receives.

## Check it

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

`capabilities` prints one row per call family and one column per class of support, then the operations a frame reported instead of answering:

```text
core context:
  family                           supported  waiting for a bridge  outside the contract          not verified
  immediate-mode                           0                     0                   162                     0
  framebuffer                             23                     0                     0                     0
  total                                  364                     0                   269                     0
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

## What the scenarios cover

`mixed-render-state` plays a frame in which native passes, wrapper state and direct OpenGL alternate, checks the order of every event against a fixed script, then generates 10,000 frames from a recorded seed and checks that the wrappers and the provider agree after each one.

`legacy-gl-contract` resolves the fixed-function entry points in a compatibility context, checks that a core context withholds them, and compares the provider calls of an immediate-mode and display-list callback with a reference that calls the provider directly.

`shared-resource-lifecycle` runs 1,000 create, reload and destroy cycles for file descriptors and for NT handles with imports that fail part of the way, rejects a different GPU and a provider without the semaphore extensions, and destroys sharing contexts out of order.

`synchronous-operations` compares a partial blit and two readbacks with pack alignments 1 and 4 against the reference, passes a failed compile, a failed link and a pending query through unchanged, and checks that a query or transform feedback that spans a native pass is reported and never answered from the provider.

`gl-differential` plays 1,000 generated scripts through the layer and directly against the provider and compares the calls and the errors.

## Not verified

- The layer has not run against Mesa/Zink or any real OpenGL implementation. Calls, state and errors are compared with a simulated provider, which proves the layer's bookkeeping and not the provider's rendering.
- The native thunks the dispatch table points to and the classes that install it into LWJGL do not exist. `DispatchInstaller` is tested through its two interfaces.
- No image has crossed between two Vulkan devices. `compatibility/interop` is tested against a backend that records imports and semaphore operations.
- No bridge for a real mod exists, and no mod has run on top of the layer.
- Windows is covered only as a handle type in the lifecycle scenario.
