package io.github.luasgraphicss.minemod.adapters.frame.calls;

import io.github.luasgraphicss.minemod.adapters.frame.answers.FrameDownload;
import io.github.luasgraphicss.minemod.adapters.frame.answers.OpenGlAnswers;
import io.github.luasgraphicss.minemod.adapters.frame.session.GameFrames;
import java.nio.IntBuffer;

public final class AnswerCalls {
    private static final int VIEWPORT = 0x0BA2;

    private AnswerCalls() {}

    public static int glGetError() {
        return OpenGlAnswers.NO_ERROR;
    }

    public static String glGetString(int name) {
        return OpenGlAnswers.string(name);
    }

    public static int glGetInteger(int name) {
        return OpenGlAnswers.integer(name);
    }

    public static void glGetInteger(int name, IntBuffer into) {
        int[] answer = name == VIEWPORT ? GameFrames.current().viewport() : new int[] {OpenGlAnswers.integer(name)};
        for (int value = 0; value < answer.length && value < into.remaining(); value++) {
            into.put(into.position() + value, answer[value]);
        }
    }

    public static void glReadPixels(int left, int bottom, int width, int height, int format, int type, IntBuffer into) {
        FrameDownload.asPackedColors(GameFrames.current().scene(), width, height, into);
    }
}
