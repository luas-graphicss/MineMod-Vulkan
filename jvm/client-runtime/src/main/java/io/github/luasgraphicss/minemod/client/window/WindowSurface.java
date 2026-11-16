package io.github.luasgraphicss.minemod.client.window;

import java.util.List;

public interface WindowSurface {
    List<String> requiredInstanceExtensions();

    long createSurface(long vulkanInstance);

    FramebufferSize framebufferSize();
}
