package io.github.luasgraphicss.minemod.compatibility.provider.identity;

public enum DriverKind {
    ZINK("Zink"),
    SOFTWARE_RASTERIZER("a software rasterizer"),
    FOREIGN("another OpenGL driver");

    private final String description;

    DriverKind(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }
}
