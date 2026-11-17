# Adapters for every release and loader

The target manifest pins 37 releases and 73 combinations of release and loader. One of them, Fabric on 1.21.1, has a build of its own with mixins written against the game. The other 72 are pinned-call adapters: each carries a plan that redirects the calls with which the game opens, presents, paces and closes its main window to `PresentationOwners`, and nothing else. `tools/verification` writes those plans from `manifests/presentation-hooks.json` and checks them against the client each release ships.

A pinned-call adapter does not draw the game. Its entry point installs a native presentation owner, so the redirected calls open the window without an OpenGL context and present a frame the renderer only clears; "The native owner" says what that owner does and what it lacks. None of the 72 adapters has been started inside Forge, NeoForge, Fabric or the launcher; what ran is listed under "What the checks cover".

## Layout

- `adapters/minecraft/mc-<release>` holds the plan in the names of the client jar.
- `adapters/forge`, `adapters/fabric`, `adapters/neoforge` and `adapters/bootstrap` hold, per release, the same plan in the names that loader runs, and build the jar a user installs.
- `adapters/loaders` holds how each loader starts a plan. `launch` reads the plan from the jar and applies it to a class file or to an ASM tree; the other five modules are the entry points below.
- `adapters/presentation` holds what the plans redirect to: `core` the contract of an owner, `glfw` and `display` the hooks and the native owners of LWJGL 3 and LWJGL 2, and `renderer` the presenter that starts the bundled renderer on a window surface.

Both kinds of module have two files: a `build.gradle.kts` that applies `minemod.release-adapter` or `minemod.loader-adapter`, and `src/main/resources/META-INF/minemod/release-plan.properties`. `settings.gradle.kts` includes every directory whose build file is exactly that.

| Loader | Names at run time | Read from |
| --- | --- | --- |
| Forge 1.12.2 and the client bootstrap | of the client jar | `manifests/presentation-hooks.json` |
| Fabric | intermediary | `net.fabricmc:intermediary` of the release, pinned by SHA-1 in the plan |
| Forge up to 1.20.4 and NeoForge 1.20.1 | SRG | `mcp_config` of the release, pinned by SHA-1 in the plan |
| Forge from 1.20.6 and NeoForge from 20.2 | of Mojang | the names the hooks manifest carries |

## How each loader starts a plan

| Loader | Releases | Entry point | Where a mismatch stops the game |
| --- | --- | --- | --- |
| LaunchWrapper | Forge 1.12.2 | `TweakClass` in the manifest names `PinnedCallsTweaker`, which registers a class transformer | When the tweaker is injected, against the classes before any transformer; again when a pinned class is loaded |
| ModLauncher 2 to 10 | Forge 1.13.2 to 1.21.1, NeoForge up to 20.4 | `PinnedCallsService` in `META-INF/services`, one class transformer per pinned class | When a pinned class is transformed |
| ModLauncher 10.1 and 11 | NeoForge from 20.5 | the same service compiled against the typed targets of that API | When a pinned class is transformed |
| Fabric Loader | Fabric 1.14 to 1.21 | `fabric.mod.json`, a Mixin configuration and `PinnedCallsPlugin`, which rewrites a class after every mixin was applied to it | When the mixins of a pinned class are applied; Fabric Loader refuses another release from `fabric.mod.json` |
| None | 1.13 and 1.13.1 | a Java agent the launcher profile adds to the unmodified client | Before the game starts: the agent checks the SHA-1 of the client and every pinned call |

A plan names the exact method around each call and how many times the call appears in it. Zero matches or a different count throws `PlanRefusedException`; no entry point skips a class it cannot match. A call that another mod already redirected counts as a mismatch.

## The native owner

Each entry point installs the owner once, before the game opens its window: `PinnedCallsTweaker` when it is injected, `PinnedCallsService` in `initialize`, `PinnedCallsPlugin` in `onLoad` and the agent in `premain`, once it accepted the client and registered its transformer.

| Releases | Owner | Window and surface |
| --- | --- | --- |
| 1.13 to 1.21.1, on LWJGL 3 | `GlfwNativeOwner` | The GLFW window of the game, created without a client API. GLFW creates the Vulkan surface. |
| 1.12.2, on LWJGL 2 | `DisplayNativeOwner` | An AWT window that stands in for `Display`. The surface is created from the native handles JAWT gives for its canvas, Xlib on Linux and Win32 on Windows. |

Both start the renderer of `jvm/client-runtime` on that surface, present one cleared frame per buffer swap, follow the vertical sync the game asks for and stop the renderer when the window closes.

The renderer is the JNI library the jar bundles. Built with `-Pminemod.sceneLibrary=<library>`, a loader adapter stages that library under `natives/<system>-<architecture>/` next to its SHA-256. A jar built without the property bundles none, and the game stops when it opens its window with `this build bundles no natives/linux-x86_64/libminemod_jni.so`. Nothing falls back to the OpenGL context of the game.

What the owner lacks:

- The game goes on calling OpenGL after its window opens, and nothing in these releases sends those calls to the compatibility layer. A launch is expected to stop at the first of them; none has been tried.
- The stand-in for `Display` is a window of 854 by 480 that answers none of the size, focus, fullscreen or input queries of LWJGL 2.
- The Win32 half of the JAWT surface has never been compiled.

On LaunchWrapper the tweaker excludes `io.github.luasgraphicss.minemod.` from the class loader of the game, so the hooks the transformed classes call are the classes the tweaker installed the owner in.

## Releases without a loader

`sh gradlew :adapters:bootstrap:mc-1.13:launcherInstall` leaves in `adapters/bootstrap/mc-1.13/build/install` the two files to copy into the directory of the launcher:

```text
versions/1.13-minemod-vulkan/1.13-minemod-vulkan.json
libraries/io/github/luasgraphicss/minemod-vulkan-bootstrap-1.13/0.1.0/minemod-vulkan-bootstrap-1.13-0.1.0.jar
```

The profile inherits `1.13`, keeps its main class and its game arguments, adds `-javaagent` with that jar and lists ASM as a library with its SHA-1 and size. The client jar is never copied or rewritten: the agent redirects the calls as the JVM defines each class. 1.13.1 works the same way.

## Refresh the plans

```sh
sh gradlew :tools:verification:run --args="hooks refresh"
sh gradlew :tools:verification:run --args="adapters refresh"
```

The first rescans the clients and rewrites `manifests/presentation-hooks.json`. The second rewrites the plans of the 36 game adapters and the 72 loader adapters, downloading the name files from `maven.fabricmc.net` and `maven.minecraftforge.net`. A module with another build file is left alone.

## What the checks cover

```sh
sh gradlew check
sh gradlew :tools:verification:run --args="check --profile adapters"
sh gradlew :tools:verification:run --args="build-matrix --manifest manifests/minecraft-targets.json"
sh gradlew :tools:verification:run --args="scenario production-transformers"
sh gradlew :tools:verification:run --args="scenario vanilla-matrix"
sh gradlew :adapters:bootstrap:mc-1.13:launcherInstall :adapters:bootstrap:mc-1.13.1:launcherInstall
sh gradlew :tools:verification:run --args="scenario client-bootstrap"
sh gradlew :adapters:presentation:glfw:check :adapters:presentation:display:check \
    -Pminemod.sceneLibrary="$PWD/build/linux-client/jni/libminemod_jni.so"
```

- `gradlew check` builds the 108 modules and fails when the jar of a loader adapter lacks its plan, its hooks or what its loader starts it from. The entry points are tested against the API of each loader with a fixture class: a real `LaunchClassLoader`, the transformer interfaces of ModLauncher 2.1.5 and 11.0.5, the plugin interface of Fabric's Mixin and `java.lang.instrument`. Those tests run on Java 8, 16, 17 and 21, except the ModLauncher 11 ones, which need Java 21.
- The `adapters` profile compares every committed plan with the hooks manifest, without the network.
- `production-transformers` downloads the 37 clients and applies each plan to its own client; `vanilla-matrix` renames the pinned classes of each client to the names of each loader and applies the plan of that loader. Both need the network. `docs/compatibility/reports/vanilla-matrix.txt` is what the second one reported.
- `client-bootstrap` starts a JVM with the JVM arguments of each built profile and the production client on the class path, loads the pinned classes without running them and reads back what the JVM defined: every pinned call must go to the hooks, the client on disk must keep its SHA-1, and the same agent must stop the JVM on the client of another release. `docs/compatibility/reports/client-bootstrap.txt` is what it reported.
- The last command needs a window system and the JNI library of the `linux-client` preset. `NativeGlfwOnWindowTest` and `NativeDisplayOnWindowTest` put that library in a jar, install the native owner over it, open a window through the pinned hooks, present four frames and close. Without the property or a display both are skipped, so `gradlew check` passes without them; the CI job `Compatibility layer on Mesa/Zink` runs them on Xvfb and lavapipe and fails when either was skipped. The tests of the LaunchWrapper, ModLauncher, Fabric and agent entry points check that each installs the owner, and the agent's that it installs none for a client it refuses. `client-bootstrap` runs `premain` in a child JVM and does not look at the owner.

What they leave out:

- The native owner presented only outside the game, on lavapipe. `docs/development/reference-machines.md` says what a real GPU still has to run.
- The clients of Forge and NeoForge are the vanilla client under the names those loaders run. Their binary patches are not applied, so a patch that adds or removes a pinned call would only be found when the game starts.
- No adapter was loaded by Forge, NeoForge, Fabric Loader or the launcher. How ModLauncher 9 and later place a service jar in their module layers is untested.
- `client-bootstrap` loads classes in a JVM of the version that runs the tool, not in the Java 8 the two releases ship with, and never calls the game.
