package io.github.luasgraphicss.minemod.codec.scene;

import java.util.Optional;

public enum SceneOpcode {
    DEFINE_TEXTURE(16, 24, false),
    WRITE_TEXTURE(17, 40, true),
    DESTROY_TEXTURE(18, 8, false),
    PUBLISH_SECTION(19, 64, true),
    UNLOAD_SECTION(20, 16, false),
    CHANGE_WORLD(21, 0, false),
    BEGIN_FRAME(22, 24, false),
    SET_VIEW(23, 136, false),
    SET_TERRAIN(24, 136, false),
    DRAW_BATCH(25, 152, true),
    END_FRAME(26, 0, false),
    SET_CLIP(27, 16, false),
    CLEAR_CLIP(28, 0, false),
    SET_TEXTURE_TRANSFORM(29, 24, false),
    SET_QUAD_ORDER(30, 8, false),
    CONNECT_SECTION_FACES(31, 24, false);

    private static final SceneOpcode[] OPCODES = values();
    private static final int FIRST_CODE = OPCODES[0].code;

    private final short code;
    private final int fixedBytes;
    private final boolean carriesBlob;

    SceneOpcode(int code, int fixedBytes, boolean carriesBlob) {
        this.code = (short) code;
        this.fixedBytes = fixedBytes;
        this.carriesBlob = carriesBlob;
    }

    public short code() {
        return code;
    }

    public int fixedBytes() {
        return fixedBytes;
    }

    public boolean carriesBlob() {
        return carriesBlob;
    }

    public static Optional<SceneOpcode> find(short code) {
        int position = code - FIRST_CODE;
        boolean isKnown = position >= 0 && position < OPCODES.length;
        return isKnown ? Optional.of(OPCODES[position]) : Optional.empty();
    }
}
