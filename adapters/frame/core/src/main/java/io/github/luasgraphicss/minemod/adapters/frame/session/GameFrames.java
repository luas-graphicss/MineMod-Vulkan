package io.github.luasgraphicss.minemod.adapters.frame.session;

import java.util.function.Consumer;

public final class GameFrames {
    private static volatile GameFrame current;

    private GameFrames() {}

    public static GameFrame current() {
        GameFrame frame = current;
        if (frame == null) {
            throw new IllegalStateException("the game called OpenGL while no window presents its frame");
        }
        return frame;
    }

    public static void run(Consumer<GameFrame> command) {
        current().run(command);
    }

    public static void open(GameFrame frame) {
        if (current != null) {
            throw new IllegalStateException("a window already presents the frame of the game");
        }
        current = frame;
    }

    public static void close(GameFrame frame) {
        if (current == frame) {
            current = null;
        }
    }
}
