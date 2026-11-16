package io.github.luasgraphicss.minemod.compatibility.translation.query;

import io.github.luasgraphicss.minemod.compatibility.resources.framebuffer.DefaultFramebufferBuffers;
import io.github.luasgraphicss.minemod.compatibility.resources.framebuffer.FramebufferBindings;
import io.github.luasgraphicss.minemod.compatibility.translation.legacy.DisplayListNames;
import io.github.luasgraphicss.minemod.compatibility.translation.names.ObjectNames;
import java.util.Objects;
import java.util.OptionalInt;

public final class BindingQueries {
    private static final int GL_LIST_BASE = 0x0B32;
    private static final int GL_DRAW_BUFFER = 0x0C01;
    private static final int GL_READ_BUFFER = 0x0C02;
    private static final int GL_DRAW_FRAMEBUFFER_BINDING = 0x8CA6;
    private static final int GL_READ_FRAMEBUFFER_BINDING = 0x8CAA;

    private final ObjectNames names;
    private final FramebufferBindings framebuffers;
    private final DefaultFramebufferBuffers defaultBuffers;
    private final DisplayListNames lists;

    public BindingQueries(
            ObjectNames names,
            FramebufferBindings framebuffers,
            DefaultFramebufferBuffers defaultBuffers,
            DisplayListNames lists) {
        this.names = Objects.requireNonNull(names, "names");
        this.framebuffers = Objects.requireNonNull(framebuffers, "framebuffers");
        this.defaultBuffers = Objects.requireNonNull(defaultBuffers, "defaultBuffers");
        this.lists = Objects.requireNonNull(lists, "lists");
    }

    public OptionalInt answeredHere(int parameter) {
        switch (parameter) {
            case GL_DRAW_FRAMEBUFFER_BINDING:
                return OptionalInt.of(framebuffers.drawBinding());
            case GL_READ_FRAMEBUFFER_BINDING:
                return OptionalInt.of(framebuffers.readBinding());
            case GL_LIST_BASE:
                return OptionalInt.of(lists.base());
            case GL_DRAW_BUFFER:
                return framebuffers.drawsToDefault()
                        ? OptionalInt.of(defaultBuffers.drawSelector())
                        : OptionalInt.empty();
            case GL_READ_BUFFER:
                return framebuffers.readsFromDefault()
                        ? OptionalInt.of(defaultBuffers.readSelector())
                        : OptionalInt.empty();
            default:
                return OptionalInt.empty();
        }
    }

    public int toVisibleValue(int parameter, int providerValue) {
        return NameBinding.kindBoundBy(parameter)
                .map(kind -> names.nameOfBacking(kind, providerValue))
                .orElse(providerValue);
    }
}
