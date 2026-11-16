# MineMod-Vulkan

A Vulkan renderer for Minecraft Java Edition 1.12.2 to 1.21.1: a native C++ core that small Java adapters drive through a versioned C ABI. The project is at its foundation. The contracts, the JNI bridge and a device harness exist; nothing renders the game yet.

MineMod-Vulkan is a fan project, not affiliated with or endorsed by Mojang or Microsoft. It contains no Minecraft code or assets.

## Status

What runs today:

- `manifests/minecraft-targets.json` pins the 37 supported releases and their 73 loader combinations, with official download hashes, launch arguments, Java runtime and LWJGL version. `tools/verification` checks it.
- `native/abi` is the C ABI: version and capability negotiation, generational handles and command streams that are validated before they run.
- `jvm/command-codec` writes and validates the same streams in Java 8 bytecode, and `jvm/native-bridge` drives native sessions over JNI on Java 8, 16, 17 and 21.
- `minemod_harness` creates a Vulkan 1.3 device, draws a textured and depth-tested scene, reads it back and presents it on a headless surface.

What does not exist yet: a game window or input on any Minecraft version, the Mesa/Zink OpenGL provider and its interop with the renderer, and any run on Windows or on a real GPU. `docs/architecture` says which decisions are backed by tests and which are still proposals.

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

Two tests carry the label `vulkan-device` and need a Vulkan driver. Without one, run `ctest --preset linux-debug -LE vulkan-device`.

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
