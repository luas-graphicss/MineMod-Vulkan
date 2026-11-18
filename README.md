# MineMod-Vulkan

A Vulkan renderer for Minecraft Java Edition 1.12.2 to 1.21.1: a native C++ core that small Java adapters drive through a versioned C ABI. The project is at its foundation. The contracts, the JNI bridge and a device harness exist; nothing renders the game yet.

MineMod-Vulkan is a fan project, not affiliated with or endorsed by Mojang or Microsoft. It contains no Minecraft code or assets.

## Status

What runs today:

- `manifests/minecraft-targets.json` pins the 37 supported releases and their 73 loader combinations, with official download hashes, launch arguments, Java runtime and LWJGL version. `tools/verification` checks it.
- `native/abi` is the C ABI: version and capability negotiation, generational handles and command streams that are validated before they run.
- `jvm/command-codec` writes and validates the same streams in Java 8 bytecode, and `jvm/native-bridge` drives native sessions over JNI on Java 8, 16, 17 and 21.
- `native/renderer/src/graph` plans a frame from the passes it declares: their order, with cycles rejected by name, the lifetime of every image, with identical images sharing a slot once the first is no longer used, and the layout transitions between uses.
- `native/renderer/src` holds the frame-side systems the graph relies on: frames in flight that are submitted without waiting for the GPU, resources retired until their last frame completes, a bounded upload arena, pipeline and sampler caches, descriptor pools that grow up to a limit, atlas packing, mip chains and texture uploads.
- The same tree validates SPIR-V before a shader module is created and checks a core shader replacement against the interface it must keep, publishes resource generations at a frame boundary, marks section jobs stale when their world, section or resources change, tracks the frame lifecycle through resize, suspension and device loss, and records frame, pass, resource and queue metrics. No game adapter drives these systems yet.
- `native/renderer/src/terrain` draws world sections in the four terrain layers: solid, cutout with and without mipmaps, and translucent. Section meshes are validated and uploaded through the arena, a superseded section job cannot publish, replaced buffers wait for the last frame that drew them, sections outside the frustum are skipped and translucent sections are drawn from the farthest. The shader samples the block atlas and the lightmap and fades into fog measured as a sphere or a cylinder. A device test composes the layers and checks the pixels; no game adapter feeds it sections yet.
- `native/renderer/src/draw` draws everything that is not terrain as vertex batches: any combination of position, color, texture, overlay, light and normal, submitted as triangles, strips, fans, quads or lines widened to a width in pixels, with opaque, translucent, additive, lightning, glint and crumbling blending, a depth bias for decals and batches that write only depth. One shader pair covers them, and elements a format leaves out are read from shared constants. Batches are staged in a bounded arena that refuses what does not fit, and a mesh that does not change, such as a sky dome, is uploaded once and drawn every frame.
- `native/renderer/src/scene` orders a frame as world, hand and interface: sky first, entities between opaque and translucent terrain, particles after it, and a fresh depth buffer for the hand and for the interface. A texture region can be updated at the start of a frame without losing the texels around it. Device tests check the composed pixels, also over several frames in flight; no game adapter submits batches yet.
- `native/interop` is the renderer's side of sharing images with the OpenGL provider on Linux. It reads the device and driver UUIDs and refuses a different GPU, asks the driver whether a format and usage can be exported, and exports the memory of a dedicated allocation as a file descriptor that has a single owner. It hands the scene attachments to the provider and takes them back: a transfer out of its queue family that signals one semaphore, and a transfer back that waits for another. Device tests run that round trip on one device.
- `compatibility/` is the OpenGL layer for mods, with `native/gl-provider` underneath on Linux. It loads Mesa's Zink from a private directory after checking the digest of every library, hands LWJGL 2 and LWJGL 3 function addresses of its own before they resolve any, and sends each call to the provider, to the layer or nowhere according to a catalog. The layer keeps virtual object names, shows the scene as framebuffer zero, reconciles wrapper state with direct calls around each callback and reports what it does not serve. Its tests draw through the Mesa installed on the machine, on lavapipe, also on attachments whose memory a second Vulkan device exports. `docs/development/compatibility-layer.md` says what they cover and what they leave out.
- `native/shader-service` turns the replacement of a core shader that a resource pack ships into SPIR-V for the renderer, on Linux. It expands `#moj_import` within limits of size, depth and count, reads the declarations of both stages with a tokenizer and maps the attributes, uniforms and samplers of the vanilla interface onto the layout of the scene shaders, runs `glslangValidator` in a child process bounded in time, memory and written bytes, reports errors against the file and line of the pack, and keeps compiled stages in a bounded cache keyed by compiler, contract and adapted source.
- A session reloads its programs as a transaction: staged SPIR-V is checked against the scene contract, the staged programs replace the published ones together between two frames, and the pipelines they retire wait for the last frame that used them. A program that is refused leaves the published generation in use. A batch names the staged program it draws with, and the session reports frame times, the time of each pass, peaks of texture and staging memory, pipelines, retired resources and the generation.
- `minemod_harness` creates a Vulkan 1.3 device, draws a textured and depth-tested scene through a planned frame, reads it back and presents it on a headless surface. `--validation` also checks synchronization.
- `jvm/client-runtime` is what a game adapter drives, in Java 8 bytecode without Minecraft types. It extracts the JNI library a mod bundles into a directory named after its SHA-256 and refuses contents that do not match it, opens a native session on a window surface, presents one frame per buffer swap with the pacing the game asks for, and publishes the lightmap texels and the section meshes of the game's compiler as scene resources. It also finds the core shaders a pack replaces, including through the files they import, compiles them off the render thread and publishes them in one reload; a reload that fails, or that a newer one superseded, changes nothing.
- `adapters/minecraft/mc-1.21.1` and `adapters/fabric/mc-1.21.1` are the first game adapter: mixins for Minecraft 1.21.1, packaged as a Fabric mod by a Loom build of their own outside the root Gradle build. The mod opens the game window without an OpenGL context, starts the native session on its surface, presents at every buffer swap, follows the window size and the vertical sync option, and hands over the lightmap and each section the game compiles. It joins the resource reload of the game to reload replaced core shaders, names the staged program in the draws it records for a core shader a pack replaced, adds the renderer's frame times, peaks, generation, last reload and the draws it left out to the debug screen, and writes the renderer's report into crash reports. The frame it presents is only cleared, and Minecraft has not been launched with it: outside the game, the same classes presented, resized and changed pacing on a GLFW window under lavapipe.
- The other 36 releases and 72 combinations of release and loader have pinned-call adapters under `adapters/minecraft`, `adapters/forge`, `adapters/fabric`, `adapters/neoforge` and `adapters/bootstrap`: each carries the plan that redirects the calls with which the game opens, presents, paces and closes its window to one owner, in the names its loader runs. `adapters/loaders` starts a plan through LaunchWrapper, ModLauncher, Fabric's Mixin or, for 1.13 and 1.13.1, which have no loader, a Java agent with a launcher profile over the unmodified client, and stops the game when a pinned call does not match. The plans were applied to the 37 production clients, and a child JVM started from the two profiles redirected every pinned call of 1.13 and 1.13.1. Each entry point also installs a native owner for the window: the GLFW window of the game without a client API on LWJGL 3, an AWT window in place of `Display` on 1.12.2. Outside the game, on lavapipe and an Xvfb display, both owners opened their window, presented cleared frames with the renderer a jar bundles and closed. These adapters draw nothing and none has run inside a loader; `docs/development/release-adapters.md` says what was checked.
- 1.12.2, 1.13, 1.13.1 and 1.13.2 have no Fabric target, so their adapters carry a second plan: it hands the fixed-pipeline OpenGL calls of the game to stand-ins in `adapters/frame`, which keep the matrices, the state and the textures the game sets and record each draw for the scene renderer instead of an OpenGL context. `tools/verification` wrote those plans from the four production clients and applied them back: 441 calls reach their stand-in, under the SRG names of Forge on 1.13.2. The stand-ins ran in tests against a scene that records draws. None of the four was launched, no recorded frame was drawn with the renderer, and the classes Forge adds still call OpenGL.
- `adapters/events`, `adapters/renderer-ownership`, `adapters/shader-packs`, `adapters/configuration` and `compatibility/bridges` hold the rules for mods next to the native frame: the render events a loader is told about and can cancel, the bridge a mod gets only on the exact version and symbols its contract names, the refusal to open a world while a renderer replacement claims a stage nobody bridges, the plan of the passes of a shader pack, and safe mode after launches that never drew. `manifests/compatibility-cases.json` lists 39 cases with their outcome and evidence. They ran with fixture mods on a simulated loader and on a backend that records passes instead of drawing; the 15 installs of published renderer mods in the list were identified by their jars and not run. One game adapter calls one of these modules: the Fabric mod for 1.21.1 asks the ownership rules before the game starts and stops the launch with the diagnosis, which Fabric Loader ran next to stand-in jars that only declare the id of a renderer. `docs/development/mod-integration.md` says what the scenarios and that check cover.

What does not exist yet: a launch of Minecraft with the mod, any drawing of the game in the frame it presents, replacements for the terrain shaders, shader replacements on Windows, more than a cleared frame outside the game for any release other than 1.21.1, a run of the other adapters inside Forge, NeoForge, Fabric or the launcher, a pinned Mesa/Zink build, the semaphore handoff between the provider and the renderer, the export of the renderer's scene attachments to the provider, a mod that draws through the layer, a third-party mod or shader pack next to the renderer, and any run on Windows or on a real GPU. `docs/architecture` says which decisions are backed by tests and which are still proposals. `manifests/reference-machines.json` lists the machines behind the evidence, one software rasterizer so far, and `docs/development/reference-machines.md` says how to record a machine and what a real GPU still has to run.

## Requirements

- JDK 21 to run Gradle. The tests also run on Java 8, 16 and 17; Gradle downloads the runtimes it does not find.
- CMake 3.22 or newer, Ninja and a C++20 compiler. GCC 11.5 and 14.2 build the tree without warnings.
- Vulkan headers and loader, `glslangValidator`, the Khronos validation layers and a Vulkan 1.3 driver. Mesa's lavapipe is enough.
- Xlib headers (`libx11-dev` or `libX11-devel`) for the `linux-client` preset, whose JNI library creates the surface of an AWT window.
- Optional: Mesa 24.2 or newer with its EGL library and Zink, for the tests of the compatibility layer that draw through a provider.
- Optional: a window system, Xvfb is enough, for the tests that present on a window.

Only Linux has been built. The Windows presets are untested.

## Build and test

The JVM side, including the JNI library it builds through the `linux-jni` preset, which leaves the renderer out and needs no Vulkan:

```sh
sh gradlew check
sh gradlew :tools:verification:run --args="check --profile foundation"
```

`gradlew` is stored without the executable bit, hence `sh gradlew`.

The tests of the compatibility layer that need Mesa are skipped unless they are pointed at one. `docs/development/compatibility-layer.md` has the commands.

The Fabric mod for 1.21.1 has a build of its own. It also builds the JNI library the mod bundles, with the renderer in it, through the `linux-client` preset:

```sh
sh gradlew -p adapters/fabric/mc-1.21.1 build
```

The jar lands in `adapters/fabric/mc-1.21.1/build/libs`. Set `-Dminemod.validation=true` on the game's JVM to run the session with the validation layers.

The adapters of the other releases build with the root project. Their plans are checked against the hooks manifest without the network and against the production clients with it; `docs/development/release-adapters.md` has the other commands:

```sh
sh gradlew :tools:verification:run --args="check --profile adapters"
sh gradlew :tools:verification:run --args="scenario vanilla-matrix"
```

The native side, with AddressSanitizer and UndefinedBehaviorSanitizer in `linux-debug`:

```sh
cmake --preset linux-debug
cmake --build --preset linux-debug
ctest --preset linux-debug
```

Seven tests carry the label `vulkan-device` and need a Vulkan driver. Without one, run `ctest --preset linux-debug -LE vulkan-device`.

The harness also runs on its own, here from the `linux-release` preset:

```sh
cmake --preset linux-release
cmake --build --preset linux-release
./build/linux-release/native/renderer/harness/minemod_harness --validation --frames 3
```

```text
device: llvmpipe (LLVM 15.0.7, 256 bits) (CPU)
driver: llvmpipe Mesa 24.2.6 (LLVM 15.0.7)
vulkan: 1.3.289
frame: 256x256, color VkFormat 37, depth VkFormat 126
presentation: 3 frame(s) on a headless surface, VkFormat 37
validation: 0 message(s)
```

## License

GPL-3.0. See `LICENSE`.
