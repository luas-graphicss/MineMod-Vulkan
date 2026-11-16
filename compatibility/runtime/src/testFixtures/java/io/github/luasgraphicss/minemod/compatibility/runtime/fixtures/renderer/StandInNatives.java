package io.github.luasgraphicss.minemod.compatibility.runtime.fixtures.renderer;

import java.nio.file.Path;

final class StandInNatives {
    private StandInNatives() {
    }

    static void load(Path library) {
        System.load(library.toAbsolutePath().toString());
    }

    static native long open();

    static native void close(long host);

    static native byte[] deviceIdentity(long host);

    static native long[] attachmentSupport(long host, boolean color, int vulkanFormat);

    static native boolean semaphoresShareable(long host);

    static native int createAttachment(long host, boolean color, int vulkanFormat, int width, int height);

    static native void destroyAttachment(long host, int attachment);

    static native long allocationSize(long host, int attachment);

    static native int exportMemory(long host, int attachment);

    static native void clearColor(long host, int attachment, int x, int y, int width, int height, int rgba);

    static native void clearDepth(long host, int attachment, int width, int height, float depth);

    static native int colorAt(long host, int attachment, int x, int y);

    static native String validationText(long host);

    static native void closeDescriptor(int descriptor);
}
