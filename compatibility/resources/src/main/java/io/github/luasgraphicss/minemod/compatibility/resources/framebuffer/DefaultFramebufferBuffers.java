package io.github.luasgraphicss.minemod.compatibility.resources.framebuffer;

public final class DefaultFramebufferBuffers {
    private static final int GL_NONE = 0;
    private static final int GL_FRONT_LEFT = 0x0400;
    private static final int GL_FRONT_RIGHT = 0x0401;
    private static final int GL_BACK_LEFT = 0x0402;
    private static final int GL_BACK_RIGHT = 0x0403;
    private static final int GL_FRONT = 0x0404;
    private static final int GL_BACK = 0x0405;
    private static final int GL_LEFT = 0x0406;
    private static final int GL_RIGHT = 0x0407;
    private static final int GL_FRONT_AND_BACK = 0x0408;
    private static final int GL_AUX0 = 0x0409;
    private static final int GL_AUX3 = 0x040C;
    private static final int GL_COLOR_ATTACHMENT0 = 0x8CE0;
    private static final int GL_COLOR_ATTACHMENT31 = 0x8CFF;
    private static final int GL_INVALID_ENUM = 0x0500;
    private static final int GL_INVALID_OPERATION = 0x0502;

    private static final BufferSelection MISSING_BUFFER = BufferSelection.rejected(GL_INVALID_OPERATION);
    private static final BufferSelection UNKNOWN_SELECTOR = BufferSelection.rejected(GL_INVALID_ENUM);

    private int drawSelector = GL_BACK;
    private int readSelector = GL_BACK;

    public BufferSelection selectDraw(int selector) {
        BufferSelection selection = classify(selector);
        if (selection.isApplied()) {
            drawSelector = selector;
        }
        return selection;
    }

    public BufferSelection selectRead(int selector) {
        BufferSelection selection = classify(selector);
        if (selection.isApplied()) {
            readSelector = selector;
        }
        return selection;
    }

    public int drawSelector() {
        return drawSelector;
    }

    public int readSelector() {
        return readSelector;
    }

    private static BufferSelection classify(int selector) {
        switch (selector) {
            case GL_NONE:
                return BufferSelection.noBuffer();
            case GL_BACK:
            case GL_BACK_LEFT:
                return BufferSelection.sceneColor();
            case GL_FRONT:
            case GL_FRONT_LEFT:
            case GL_LEFT:
            case GL_FRONT_AND_BACK:
                return BufferSelection.frontBufferUnavailable();
            case GL_RIGHT:
            case GL_FRONT_RIGHT:
            case GL_BACK_RIGHT:
                return MISSING_BUFFER;
            default:
                return namesAnAbsentBuffer(selector) ? MISSING_BUFFER : UNKNOWN_SELECTOR;
        }
    }

    private static boolean namesAnAbsentBuffer(int selector) {
        return (selector >= GL_AUX0 && selector <= GL_AUX3)
                || (selector >= GL_COLOR_ATTACHMENT0 && selector <= GL_COLOR_ATTACHMENT31);
    }
}
