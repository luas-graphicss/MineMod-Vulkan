package io.github.luasgraphicss.minemod.verification.differential.simulated;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import io.github.luasgraphicss.minemod.verification.differential.ScriptTarget;
import io.github.luasgraphicss.minemod.verification.scenario.simulation.GlDriver;
import java.util.Optional;

final class SimulatedTarget implements ScriptTarget {
    private static final long GL_UNSIGNED_BYTE = 0x1401;
    private static final long GL_RGBA = 0x1908;
    private static final int FIRST_UNUSED_NAME = 400;

    private final GlDriver gl;

    SimulatedTarget(GlDriver gl) {
        this.gl = gl;
    }

    @Override
    public int generate(ObjectKind kind) {
        return gl.generate(kind, 1)[0];
    }

    @Override
    public void delete(ObjectKind kind, int name) {
        gl.delete(kind, name);
    }

    @Override
    public boolean isObject(ObjectKind kind, int name) {
        return gl.isObject(kind, name);
    }

    @Override
    public void call(String function, long... arguments) {
        gl.call(function, arguments);
    }

    @Override
    public Optional<String> readPixel() {
        gl.call("glReadPixels", 0, 0, 1, 1, GL_RGBA, GL_UNSIGNED_BYTE, 0);
        return Optional.empty();
    }

    @Override
    public int takeError() {
        return gl.takeError();
    }

    @Override
    public String observedName(int slot, int name) {
        return Integer.toString(name);
    }

    @Override
    public int unusedName(int index) {
        return FIRST_UNUSED_NAME + index;
    }
}
