# Mods next to the native renderer

Five modules hold the rules a game adapter needs before mods take part in a native frame: which render events a loader is told about, which bridge may serve a mod, who owns each stage of the pipeline, how the passes of a shader pack are planned, and what happens to the settings after a launch that never drew. `tools/verification` calls all five. The Fabric mod for 1.21.1 calls one, the ownership rules, before the game starts; no game adapter calls the other four.

## What each module decides

- `adapters/events` walks the points of a frame in order through `FrameEvents`. A `LoaderEventContract` says which points a loader is notified of, which ones it can cancel and where it is asked element by element. A native draw at a cancelable point has to ask `permits` first; announcing the point instead throws. `ResourceGenerations` refuses to publish a resource snapshot while an extension point that mods add to has not completed for that reload.
- `compatibility/bridges` activates a bridge only when its `BridgeContract` holds: the mod is installed, its exact version is listed and every symbol the contract names (owner, name and descriptor) is present. The verdict says which of the three failed.
- `adapters/renderer-ownership` reads the installed mods against `renderer-families.properties`, which lists canvas, embeddium, iris, oculus, optifabric, optifine, rubidium, sodium and vulkanmod with the stages each replaces. `WorldLoadGate.requireOpen()` throws `RendererConflictException` with a diagnosis when one of them claims a stage that no bridge serves for that exact version, or when two claim the same stage. Presentation and frame submission accept no bridge.
- `adapters/shader-packs` reads a pack within limits of file count and size, expands its includes and plans a frame: shadows, geometry programs, deferred and composite passes, buffer flips, depth snapshots and buffers kept from one frame to the next. A pack that needs compute is refused when `PipelineCapabilities` has none, and a pack with problems returns them instead of a partial plan. `ShaderPipeline` runs the plan on a `PipelineBackend`.
- `adapters/configuration` replaces the settings file through a staged file and keeps, in a journal, the phase each of the last eight launches reached. When the launches before the current one ended before the first frame, `RecoveryPolicy` starts safe mode: no shader pack, and the diagnostic overlay on.

## What the Fabric mod for 1.21.1 asks

`RendererOwnershipCheck` is the `preLaunch` entry point of the mod, so it runs before Fabric Loader starts the game. It reads the id and the version of every mod Fabric Loader found and negotiates them against `renderer-families.properties`, with no bridge. An id that a mod only declares in `provides` counts as installed, under a version that names the mod providing it, so a bridge written for the real renderer cannot serve its stand-in. When the gate stays closed, `requireOpen()` throws and Fabric Loader stops the launch with the diagnosis: no window opens, the two renderers never share a frame and neither is switched off.

Fabric Loader 0.20.1 started the mod in the development environment of the Loom build, without the native libraries and the assets, next to one stand-in jar in `run/mods` at a time:

```sh
sh gradlew -p adapters/fabric/mc-1.21.1 :runClient -x configureNativeLibrary -x buildNativeLibrary -x stageNativeLibrary -x downloadAssets
```

- A jar whose `fabric.mod.json` declares only the id `sodium`: the launch stopped at `preLaunch` with `TERRAIN_GEOMETRY has no bridge for this exact renderer version, claimed by [sodium 0.5.11+mc1.21.1]` and the same for `TERRAIN_DRAW`.
- A jar that provides `iris` under another id: it stopped with `SHADER_PIPELINE has no bridge for this exact renderer version, claimed by [iris 9.9.9 provided by standin-shader-loader]`.
- A jar that names no renderer: the launch went on into the game, which stopped at the client entry point because that run bundles no native library.

## What the scenarios run

```sh
sh gradlew :tools:verification:run --args="scenario modpack-matrix"
sh gradlew :tools:verification:run --args="scenario renderer-conflicts"
sh gradlew :tools:verification:run --args="scenario shader-integration"
sh gradlew :tools:verification:run --args="check --profile ecosystem"
```

- `modpack-matrix` runs 20 packs of nine fixture mods, one per category (inventory, overlay, minimap, models, machines, a scheduler, a chunk renderer, a shader loader and a recorder), on a simulated loader: a priority bus for Forge and NeoForge, a callback array for Fabric. Each pack draws 6 frames in 2 worlds through 4 bridges, with a shader pack. One more pack carries the chunk renderer in a version no bridge names and is refused before the world. A recovery drill on one pack begins two launches that show no frame: the third runs in safe mode without the shader pack, the settings file and a world file keep their bytes, and the launch after a shown frame runs the chosen pack again.
- `renderer-conflicts` negotiates 15 installs of published renderer mods, each identified by the file name and the SHA-1 of its jar. Fourteen need a bridge. VulkanMod is unsupported, because it replaces presentation and frame submission. The jars were read, not run.
- `shader-integration` plans four fixture packs on a backend that records the order of passes instead of drawing: chained passes with shadows and depth, a history buffer, a compute pass with and without compute, and a custom uniform that is refused.

`manifests/compatibility-cases.json` lists the 39 cases with the exact mod versions, the capability, the scenario, the outcome (`verified`, `requires-bridge`, `unsupported` or `unverified`) and the reason, and pins each report under `docs/compatibility/reports` by SHA-256. The `ecosystem` profile fails when a report on disk is not what its scenario writes. After changing a scenario, rewrite the reports and the pins:

```sh
sh gradlew :tools:verification:run --args="evidence refresh"
```

## What they leave out

- No third-party mod and no shader pack ran. `verified` in the manifest means the fixtures passed, and each reason says so.
- The event contracts are fixtures that model a priority bus and a callback array. No contract names the event classes of Forge, NeoForge or Fabric yet.
- No pass reached a GPU: the backend of the shader scenario is symbolic.
- No bridge for a published renderer exists, so every one of them keeps the world closed and, on Fabric 1.21.1, stops the launch.
- The check of the Fabric mod ran next to stand-in jars that carry nothing but a `fabric.mod.json`. It has not run next to a published renderer mod, nor from the jar a user installs.
- No game adapter calls `adapters/events`, `adapters/shader-packs`, `adapters/configuration` or `compatibility/bridges`.
