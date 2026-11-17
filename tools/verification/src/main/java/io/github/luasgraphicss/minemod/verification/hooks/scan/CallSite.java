package io.github.luasgraphicss.minemod.verification.hooks.scan;

import io.github.luasgraphicss.minemod.verification.hooks.catalog.LibraryCall;

public record CallSite(LibraryCall call, String owner, String method, String descriptor) {
    public String within() {
        return owner + "." + method + descriptor;
    }
}
