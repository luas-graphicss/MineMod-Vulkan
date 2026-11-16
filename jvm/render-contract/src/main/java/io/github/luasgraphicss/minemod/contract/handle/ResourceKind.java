package io.github.luasgraphicss.minemod.contract.handle;

public enum ResourceKind {
    BUFFER(1);

    private final int code;

    ResourceKind(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    static ResourceKind fromCode(int code) {
        for (ResourceKind kind : values()) {
            if (kind.code == code) {
                return kind;
            }
        }
        throw new IllegalArgumentException("unknown resource kind: " + code);
    }
}
