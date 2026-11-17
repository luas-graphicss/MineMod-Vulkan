package io.github.luasgraphicss.minemod.adapters.presentation.glfw.surface;

import io.github.luasgraphicss.minemod.client.failure.RendererFailure;
import io.github.luasgraphicss.minemod.client.window.FramebufferSize;
import io.github.luasgraphicss.minemod.client.window.WindowSurface;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

public final class GlfwWindowSurface implements WindowSurface {
    private static final int VULKAN_SUCCESS = 0;

    private final long window;

    public GlfwWindowSurface(long window) {
        this.window = window;
    }

    @Override
    public List<String> requiredInstanceExtensions() {
        if (!GLFWVulkan.glfwVulkanSupported()) {
            throw new RendererFailure("GLFW found no Vulkan loader on this system");
        }
        PointerBuffer names = GLFWVulkan.glfwGetRequiredInstanceExtensions();
        if (names == null) {
            throw new RendererFailure("GLFW cannot create Vulkan surfaces on this window system");
        }
        List<String> extensions = new ArrayList<>(names.remaining());
        for (int index = names.position(); index < names.limit(); index++) {
            extensions.add(MemoryUtil.memASCII(names.get(index)));
        }
        return extensions;
    }

    @Override
    public long createSurface(long vulkanInstance) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer surface = stack.mallocLong(1);
            int result = GLFWVulkan.nglfwCreateWindowSurface(
                    vulkanInstance, window, MemoryUtil.NULL, MemoryUtil.memAddress(surface));
            if (result != VULKAN_SUCCESS) {
                throw new RendererFailure("GLFW created no Vulkan surface for the game window: VkResult " + result);
            }
            return surface.get(0);
        }
    }

    @Override
    public FramebufferSize framebufferSize() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            GLFW.glfwGetFramebufferSize(window, width, height);
            return new FramebufferSize(width.get(0), height.get(0));
        }
    }
}
