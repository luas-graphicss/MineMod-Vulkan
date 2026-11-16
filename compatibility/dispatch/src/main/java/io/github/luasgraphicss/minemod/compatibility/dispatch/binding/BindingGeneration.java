package io.github.luasgraphicss.minemod.compatibility.dispatch.binding;

public enum BindingGeneration {
    LWJGL2("org.lwjgl.opengl.GLContext"),
    LWJGL3("org.lwjgl.opengl.GL");

    private final String capabilitiesOwner;

    BindingGeneration(String capabilitiesOwner) {
        this.capabilitiesOwner = capabilitiesOwner;
    }

    public String capabilitiesOwner() {
        return capabilitiesOwner;
    }
}
