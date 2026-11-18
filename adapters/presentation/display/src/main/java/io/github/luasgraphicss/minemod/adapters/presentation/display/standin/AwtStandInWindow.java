package io.github.luasgraphicss.minemod.adapters.presentation.display.standin;

import io.github.luasgraphicss.minemod.adapters.presentation.nativeframe.StandInWindow;
import io.github.luasgraphicss.minemod.client.window.WindowSurface;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;

public final class AwtStandInWindow implements StandInWindow {
    private static final String TITLE = "Minecraft";
    private static final int WIDTH = 854;
    private static final int HEIGHT = 480;

    private Frame frame;
    private Canvas canvas;

    @Override
    public void open() {
        if (frame != null) {
            throw new IllegalStateException("the stand-in for the LWJGL 2 display is already open");
        }
        Canvas content = new Canvas();
        content.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        content.setBackground(Color.BLACK);
        content.setIgnoreRepaint(true);
        Frame opened = new Frame(TITLE);
        opened.setIgnoreRepaint(true);
        opened.add(content);
        opened.pack();
        opened.setVisible(true);
        frame = opened;
        canvas = content;
    }

    public WindowSurface surface() {
        if (canvas == null) {
            throw new IllegalStateException("the stand-in for the LWJGL 2 display is not open");
        }
        return new AwtCanvasSurface(canvas);
    }

    @Override
    public void close() {
        if (frame == null) {
            return;
        }
        frame.dispose();
        frame = null;
        canvas = null;
    }
}
