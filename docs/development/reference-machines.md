# Reference machines

A result counts as evidence only together with the machine that produced it. `manifests/reference-machines.json` lists those machines: the system, the Vulkan device the renderer selected, its driver and the evidence that ran there. `tools/verification` writes the entries and checks them.

## What an entry records

| Evidence | What ran on the machine |
| --- | --- |
| `native-presentation` | `minemod_harness --validation --frames 3` presented its frames with zero validation messages. |
| `window-presentation` | `NativeGlfwOnWindowTest` and `NativeDisplayOnWindowTest` ran every test: the native owners opened a GLFW window and the stand-in for the LWJGL 2 display, presented on both and closed them. |
| `shared-image-interop` | `SharedSceneOnZinkTest` ran every test: a native draw, an OpenGL draw through Zink and a second native draw on one image. |

The device, its type, the driver and the Vulkan version are read from the report of the harness, so an entry names the device the renderer picks on that machine and not one somebody typed. A suite counts only when none of its tests failed or was skipped on any Java runtime that ran it. The second condition is the reason the tool reads the reports: without Mesa or a window system those suites are skipped and `gradlew check` still passes.

## Record a machine

Build the harness and the two JNI libraries, run the three suites on a machine with a window system, then record. This is the run behind the `software-lavapipe` entry:

```sh
cmake --preset linux-release
cmake --build --preset linux-release
cmake --preset linux-client
cmake --build --preset linux-client
cmake --build --preset linux-scene-host
sh gradlew :adapters:presentation:glfw:check :adapters:presentation:display:check :compatibility:runtime:check \
    -Pminemod.systemProvider=/usr/lib64 \
    -Pminemod.sceneHost="$PWD/build/linux-client/jni/libminemod_scene_host_jni.so" \
    -Pminemod.sceneLibrary="$PWD/build/linux-client/jni/libminemod_jni.so"
sh gradlew :tools:verification:run --args="machines record --id software-lavapipe \
    --harness build/linux-release/native/renderer/harness/minemod_harness --results . \
    --system 'Amazon Linux 2023, x86-64, in a container'"
```

```text
machine software-lavapipe: llvmpipe (LLVM 15.0.7, 256 bits) (cpu) on linux, native-presentation, window-presentation, shared-image-interop
```

`docs/development/compatibility-layer.md` explains the three properties. `--results` names a directory that is searched for the JUnit reports of the suites; without it only the harness is recorded. `--system` replaces the description the JVM gives, such as `Linux 6.18.56, amd64`, when the entry should name the distribution or the OS build. An id that is already listed has its entry replaced.

The command says which evidence it left out and why. Without `--results`:

```text
  not recorded, window-presentation: NativeGlfwOnWindowTest has no report; NativeDisplayOnWindowTest has no report
  not recorded, shared-image-interop: SharedSceneOnZinkTest has no report
machine software-lavapipe: llvmpipe (LLVM 15.0.7, 256 bits) (cpu) on linux, native-presentation
```

On a machine with more than one Vulkan device, check that the suites drew on the device the harness printed. The entry names only that one, and it is wrong if a variable such as `MESA_VK_DEVICE_SELECT` sent the suites to another.

## The gate for hardware

```sh
sh gradlew :tools:verification:run --args="check --profile hardware"
```

The profile fails until an integrated or discrete GPU has recorded the three kinds of evidence on Linux, and one on Windows. A software rasterizer and a virtual GPU do not count. Today it prints:

```text
check reference-machines: ok
check hardware-evidence: 2 violation(s)
  - no machine with a real GPU on linux recorded native-presentation, window-presentation, shared-image-interop
  - no machine with a real GPU on windows recorded native-presentation, window-presentation, shared-image-interop
profile hardware: 1 passed, 1 failed
```

CI does not run this profile. The `foundation` profile, which CI runs, checks the form of the manifest.

No Windows entry can be recorded yet. CI builds only `windows-provider` there, which leaves the renderer and its harness out, the provider tests of Gradle are wired to the Linux JNI library, and the GLFW suite loads only the Linux natives of LWJGL.

## software-lavapipe

The only machine so far. It has no GPU.

- System: Amazon Linux 2023, x86-64, in a container.
- Vulkan: llvmpipe (LLVM 15.0.7, 256 bits) from Mesa 24.2.6, API 1.3.289, Khronos validation layers 1.3.296.
- Native toolchain: GCC 11.5.0, CMake 3.22.2, Ninja 1.10.2, glslang 15.0.0. GCC 14.2.1 also builds the tree without warnings.
- Java: Corretto 8u512, Temurin 16.0.2, Corretto 17.0.21 and Corretto 21.0.13.
- Passed: `gradlew check`, `ctest --preset linux-debug` under AddressSanitizer and UndefinedBehaviorSanitizer, and `ctest --preset linux-release`.
- Recorded: the three kinds of evidence, with the windows on an Xvfb display and Zink from the Mesa of the system. The window suites ran on the four Java runtimes.

A software rasterizer shows that the code agrees with the Vulkan specification and the validation layers. It settles nothing about real drivers, presentation through a compositor or interop between two devices on one GPU. Those need hardware entries for Windows and Linux.
