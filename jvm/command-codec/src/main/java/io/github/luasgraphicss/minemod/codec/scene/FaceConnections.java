package io.github.luasgraphicss.minemod.codec.scene;

public final class FaceConnections {
    private static final int FACE_COUNT = SectionFace.values().length;
    private static final FaceConnections NONE = new FaceConnections(0);
    private static final FaceConnections OPEN = new FaceConnections((1L << (FACE_COUNT * FACE_COUNT)) - 1);

    private final long pairs;

    private FaceConnections(long pairs) {
        this.pairs = pairs;
    }

    public static FaceConnections none() {
        return NONE;
    }

    public static FaceConnections open() {
        return OPEN;
    }

    public FaceConnections connecting(SectionFace first, SectionFace second) {
        return new FaceConnections(pairs | pairBit(first, second) | pairBit(second, first));
    }

    long pairs() {
        return pairs;
    }

    private static long pairBit(SectionFace first, SectionFace second) {
        return 1L << (first.code() * FACE_COUNT + second.code());
    }
}
