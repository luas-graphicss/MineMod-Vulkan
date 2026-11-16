# Reference machines

A result counts as evidence only together with the machine that produced it. Every machine that runs `gradlew check` and the native presets gets an entry here. A hardware entry also records the GPU, the driver version and the OS build.

## software-lavapipe

The only machine so far. It has no GPU.

- System: Amazon Linux 2023, x86-64, in a container.
- Vulkan: llvmpipe (LLVM 15.0.7, 256 bits) from Mesa 24.2.6, API 1.3.289, Khronos validation layers 1.3.296.
- Native toolchain: GCC 11.5.0, CMake 3.22.2, Ninja 1.10.2, glslang 15.0.0. GCC 14.2.1 also builds the tree without warnings.
- Java: Corretto 8u512, Temurin 16.0.2, Corretto 17.0.21 and Corretto 21.0.13.
- Passed: `gradlew check`, `ctest --preset linux-debug` under AddressSanitizer and UndefinedBehaviorSanitizer, and `ctest --preset linux-release`.

A software rasterizer shows that the code agrees with the Vulkan specification and the validation layers. It settles nothing about real drivers, presentation to a window or interop between two devices on one GPU. Those need hardware entries for Windows and Linux.
