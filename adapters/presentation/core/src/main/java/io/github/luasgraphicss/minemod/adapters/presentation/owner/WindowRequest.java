package io.github.luasgraphicss.minemod.adapters.presentation.owner;

import java.util.Objects;

public final class WindowRequest {
    public static final long DISPLAY_WINDOW = 0L;
    private static final int UNSIZED = 0;
    private final int width;
    private final int height;
    private final String title;
    private final boolean display;

    private WindowRequest(int width, int height, String title, boolean display) {
        this.width = width;
        this.height = height;
        this.title = title;
        this.display = display;
    }

    public static WindowRequest ofSize(int width, int height, CharSequence title) {
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException("a window is at least 1x1, not " + width + "x" + height);
        }
        return new WindowRequest(width, height, Objects.requireNonNull(title, "title").toString(), false);
    }

    public static WindowRequest ofDisplay() {
        return new WindowRequest(UNSIZED, UNSIZED, "", true);
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public String title() {
        return title;
    }

    public boolean isDisplay() {
        return display;
    }

    @Override
    public String toString() {
        return display ? "the LWJGL 2 display" : "window \"" + title + "\" " + width + "x" + height;
    }
}
