package io.github.luasgraphicss.minemod.compatibility.translation.framebuffer;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import io.github.luasgraphicss.minemod.compatibility.contracts.support.OperationSupport;
import io.github.luasgraphicss.minemod.compatibility.dispatch.catalog.CallCatalog;
import io.github.luasgraphicss.minemod.compatibility.dispatch.catalog.CallDescription;
import io.github.luasgraphicss.minemod.compatibility.dispatch.catalog.Interception;
import io.github.luasgraphicss.minemod.compatibility.resources.framebuffer.BufferSelection;
import io.github.luasgraphicss.minemod.compatibility.resources.framebuffer.DefaultFramebufferBuffers;
import io.github.luasgraphicss.minemod.compatibility.resources.framebuffer.FramebufferBindings;
import io.github.luasgraphicss.minemod.compatibility.resources.framebuffer.FramebufferTarget;
import io.github.luasgraphicss.minemod.compatibility.state.error.ErrorLedger;
import io.github.luasgraphicss.minemod.compatibility.translation.names.NameArguments;
import io.github.luasgraphicss.minemod.compatibility.translation.names.ObjectNames;
import io.github.luasgraphicss.minemod.compatibility.translation.provider.ProviderCalls;
import io.github.luasgraphicss.minemod.compatibility.translation.report.OperationReporter;
import java.util.Objects;
import java.util.Optional;
import java.util.function.IntFunction;

public final class VirtualFramebufferCalls {
    public static final int REJECTED = -1;

    private static final String BIND = "glBindFramebuffer";
    private static final String DRAW_BUFFER = "glDrawBuffer";
    private static final String READ_BUFFER = "glReadBuffer";
    private static final int GL_INVALID_ENUM = 0x0500;
    private static final int GL_INVALID_OPERATION = 0x0502;
    private static final int GL_BACK_LEFT = 0x0402;
    private static final int GL_DEPTH = 0x1801;
    private static final int GL_STENCIL = 0x1802;
    private static final int GL_COLOR_ATTACHMENT0 = 0x8CE0;
    private static final int GL_DEPTH_ATTACHMENT = 0x8D00;
    private static final int GL_STENCIL_ATTACHMENT = 0x8D20;

    private final FramebufferBindings bindings;
    private final DefaultFramebufferBuffers buffers;
    private final ObjectNames names;
    private final NameArguments nameArguments;
    private final CallCatalog catalog;
    private final ProviderCalls provider;
    private final ErrorLedger errors;
    private final OperationReporter reporter;

    public VirtualFramebufferCalls(
            FramebufferBindings bindings,
            DefaultFramebufferBuffers buffers,
            ObjectNames names,
            CallCatalog catalog,
            ProviderCalls provider,
            ErrorLedger errors,
            OperationReporter reporter) {
        this.bindings = Objects.requireNonNull(bindings, "bindings");
        this.buffers = Objects.requireNonNull(buffers, "buffers");
        this.names = Objects.requireNonNull(names, "names");
        this.nameArguments = new NameArguments(names);
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.provider = Objects.requireNonNull(provider, "provider");
        this.errors = Objects.requireNonNull(errors, "errors");
        this.reporter = Objects.requireNonNull(reporter, "reporter");
    }

    public void bind(int targetCode, int name) {
        Optional<FramebufferTarget> target = FramebufferTarget.fromCode(targetCode);
        if (!target.isPresent()) {
            errors.raise(GL_INVALID_ENUM);
            return;
        }
        if (name != FramebufferBindings.DEFAULT_FRAMEBUFFER
                && names.backingForUse(ObjectKind.FRAMEBUFFER, name) == ObjectNames.REJECTED) {
            return;
        }
        provider.invoke(BIND, new long[] {targetCode, bindings.bind(target.get(), name)});
    }

    public void delete(int[] framebuffers) {
        for (int name : framebuffers) {
            for (FramebufferTarget reverted : bindings.revertBindingsOf(name)) {
                long sceneBacking = bindings.backingOf(FramebufferBindings.DEFAULT_FRAMEBUFFER);
                provider.invoke(BIND, new long[] {reverted.code(), sceneBacking});
            }
        }
        names.delete(ObjectKind.FRAMEBUFFER, framebuffers);
    }

    public void drawBuffer(int selector) {
        select(DRAW_BUFFER, selector, bindings.drawsToDefault(), buffers::selectDraw);
    }

    public void readBuffer(int selector) {
        select(READ_BUFFER, selector, bindings.readsFromDefault(), buffers::selectRead);
    }

    public boolean drawBuffersOnDefault(int[] selectors) {
        if (!bindings.drawsToDefault()) {
            return false;
        }
        if (selectors.length != 1) {
            errors.raise(GL_INVALID_OPERATION);
        } else {
            drawBuffer(selectors[0]);
        }
        return true;
    }

    public void attach(String function, long[] arguments) {
        CallDescription description = catalog.describe(function)
                .filter(described -> described.interception() == Interception.FRAMEBUFFER)
                .orElseThrow(() -> new IllegalArgumentException(function + " does not attach to a framebuffer"));
        Optional<FramebufferTarget> target = FramebufferTarget.fromCode((int) arguments[0]);
        if (!target.isPresent()) {
            errors.raise(GL_INVALID_ENUM);
            return;
        }
        if (isDefault(target.get())) {
            errors.raise(GL_INVALID_OPERATION);
            return;
        }
        long[] translated = arguments.clone();
        if (nameArguments.translate(description, translated)) {
            provider.invoke(function, translated);
        }
    }

    public int providerAttachment(int targetCode, int attachment) {
        Optional<FramebufferTarget> target = FramebufferTarget.fromCode(targetCode);
        if (!target.isPresent()) {
            errors.raise(GL_INVALID_ENUM);
            return REJECTED;
        }
        if (!isDefault(target.get())) {
            return attachment;
        }
        switch (attachment) {
            case GL_BACK_LEFT:
                return GL_COLOR_ATTACHMENT0;
            case GL_DEPTH:
                return GL_DEPTH_ATTACHMENT;
            case GL_STENCIL:
                return GL_STENCIL_ATTACHMENT;
            default:
                errors.raise(GL_INVALID_ENUM);
                return REJECTED;
        }
    }

    private boolean isDefault(FramebufferTarget target) {
        return target.affectsDraw() ? bindings.drawsToDefault() : bindings.readsFromDefault();
    }

    private void select(String function, int selector, boolean onDefault, IntFunction<BufferSelection> selecting) {
        if (!onDefault) {
            provider.invoke(function, new long[] {selector});
            return;
        }
        BufferSelection selection = selecting.apply(selector);
        if (selection.isApplied()) {
            provider.invoke(function, new long[] {selection.providerBuffer()});
        } else if (selection.outcome() == BufferSelection.Outcome.FRONT_BUFFER_UNAVAILABLE) {
            reporter.report(function + " on the front buffer", OperationSupport.OUT_OF_CONTRACT);
            errors.raise(GL_INVALID_OPERATION);
        } else {
            errors.raise(selection.error());
        }
    }
}
