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
- `native/interop` is the renderer's side of sharing images with the OpenGL provider on Linux. It reads the device and driver UUIDs and refuses a different GPU, asks the driver whether a format and usage can be exported, and exports the memory of a dedicated allocation as a file descriptor that has a single owner. Nothing imports that memory yet.
- `minemod_harness` creates a Vulkan 1.3 device, draws a textured and depth-tested scene through a planned frame, reads it back and presents it on a headless surface. `--validation` also checks synchronization.

What does not exist yet: a game window or input on any Minecraft version, the Mesa/Zink OpenGL provider, the import and the semaphore handoff of its interop with the renderer, and any run on Windows or on a real GPU. `docs/architecture` says which decisions are backed by tests and which are still proposals.

## Requirements

- JDK 21 to run Gradle. The tests also run on Java 8, 16 and 17; Gradle downloads the runtimes it does not find.
- CMake 3.22 or newer, Ninja and a C++20 compiler. GCC 11.5 and 14.2 build the tree without warnings.
- Vulkan headers and loader, `glslangValidator`, the Khronos validation layers and a Vulkan 1.3 driver. Mesa's lavapipe is enough.

Only Linux has been built. The Windows presets are untested.

## Build and test

The JVM side, including the JNI library it builds through the `linux-jni` preset:

```sh
sh gradlew check
sh gradlew :tools:verification:run --args="check --profile foundation"
```

`gradlew` is stored without the executable bit, hence `sh gradlew`.

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
