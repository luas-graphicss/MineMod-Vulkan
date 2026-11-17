# 6. One owner for the main window across LWJGL 2 and 3

Status: proposed. Only headless presentation has run.

## Context

Minecraft 1.12.2 creates its window through `Display` in LWJGL 2, which also creates an OpenGL context. Later versions use GLFW through LWJGL 3. A Vulkan surface cannot be created on a window whose OpenGL context already presents to it, and two copies of a windowing library cannot share one window pointer.

## Proposal

- The Vulkan presentation chain is the only presenter of the main window. OpenGL contexts used for compatibility are offscreen.
- On LWJGL 3 targets the adapter asks the game's own GLFW for a window without a client API and passes native window handles to the core. The core never loads a second GLFW to reuse that pointer.
- On 1.12.2 an adapter stands in for `Display`, `Keyboard` and `Mouse`, stops the original context from being created and keeps what the game observes: events, cursor grab, focus, clipboard, fullscreen, resolution, DPI and close.
- A windowing library used on the 1.12.2 path keeps its symbols and its lifetime to itself.

## Evidence so far

`minemod_harness` creates a swapchain on a `VK_EXT_headless_surface` surface and presents frames with zero validation messages. That exercises acquire, blit and present. It involves no window, no input and no game.

Every release has the calls with which the game opens, presents, paces and closes its window pinned and redirected to one owner, in the names each loader runs. For 1.13 and 1.13.1 a Java agent does it in memory from a launcher profile that inherits the unmodified version: a child JVM started with that profile redirected every pinned call of both production clients and left the client jars untouched. `docs/development/release-adapters.md` says what those checks leave out. No owner other than the windowing library of the game is installed outside 1.21.1.

## Needed before acceptance

- A minimal bootstrap on 1.21.1 with Fabric and on 1.12.2 with Forge: a window without the vanilla context, resize, minimise, focus, keyboard, captured mouse and close.
- A launch of 1.13 and 1.13.1 from the launcher with the profile of the agent, on the Java 8 those releases ship with.
- A check that no run loads both LWJGL generations.
