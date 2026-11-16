package io.github.luasgraphicss.minemod.client.compatibility.sharing;

import java.util.List;

public final class SharingRefused extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public SharingRefused(List<String> rejections) {
        super("the renderer and the OpenGL provider cannot share the scene: " + String.join("; ", rejections));
    }
}
