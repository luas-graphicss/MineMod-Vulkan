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

## Needed before acceptance

- A minimal bootstrap on 1.21.1 with Fabric and on 1.12.2 with Forge: a window without the vanilla context, resize, minimise, focus, keyboard, captured mouse and close.
- The launch path of 1.13 and 1.13.1, which have no loader, without redistributing the game or touching its authentication.
- A check that no run loads both LWJGL generations.
