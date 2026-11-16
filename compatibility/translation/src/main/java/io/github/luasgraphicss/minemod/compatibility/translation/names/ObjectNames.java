package io.github.luasgraphicss.minemod.compatibility.translation.names;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import io.github.luasgraphicss.minemod.compatibility.provider.capability.ContextProfile;
import io.github.luasgraphicss.minemod.compatibility.resources.names.ContextNames;
import io.github.luasgraphicss.minemod.compatibility.resources.names.NameTable;
import io.github.luasgraphicss.minemod.compatibility.state.error.ErrorLedger;
import io.github.luasgraphicss.minemod.compatibility.translation.provider.ProviderObjects;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

public final class ObjectNames {
    public static final int REJECTED = -1;

    private static final int NO_NAME = 0;
    private static final int NO_PARAMETER = 0;
    private static final int GL_INVALID_VALUE = 0x0501;
    private static final int GL_INVALID_OPERATION = 0x0502;
    private static final int GL_OUT_OF_MEMORY = 0x0505;
    private static final Set<ObjectKind> CREATED_BY_FIRST_USE =
            Collections.unmodifiableSet(EnumSet.of(ObjectKind.TEXTURE, ObjectKind.BUFFER, ObjectKind.DISPLAY_LIST));

    private final Supplier<ContextNames> currentNames;
    private final ProviderObjects provider;
    private final ErrorLedger errors;
    private final LongSupplier currentFrame;
    private final ContextProfile profile;

    public ObjectNames(
            Supplier<ContextNames> currentNames,
            ProviderObjects provider,
            ErrorLedger errors,
            LongSupplier currentFrame,
            ContextProfile profile) {
        this.currentNames = Objects.requireNonNull(currentNames, "currentNames");
        this.provider = Objects.requireNonNull(provider, "provider");
        this.errors = Objects.requireNonNull(errors, "errors");
        this.currentFrame = Objects.requireNonNull(currentFrame, "currentFrame");
        this.profile = Objects.requireNonNull(profile, "profile");
    }

    public int[] generate(ObjectKind kind, int count) {
        if (count < 0) {
            errors.raise(GL_INVALID_VALUE);
            return new int[0];
        }
        int[] names = new int[count];
        for (int index = 0; index < count; index++) {
            names[index] = create(kind, NO_PARAMETER);
        }
        return names;
    }

    public int create(ObjectKind kind, int parameter) {
        int backing = provider.create(kind, parameter);
        try {
            return table(kind).allocate(backing);
        } catch (IllegalStateException exhausted) {
            provider.dispose(kind, backing);
            errors.raise(GL_OUT_OF_MEMORY);
            return NO_NAME;
        }
    }

    public void delete(ObjectKind kind, int[] names) {
        NameTable table = table(kind);
        for (int name : names) {
            if (table.contains(name)) {
                table.delete(name, provider);
            }
        }
    }

    public boolean isLive(ObjectKind kind, int name) {
        return table(kind).contains(name);
    }

    public int backingForUse(ObjectKind kind, int name) {
        if (name == NO_NAME) {
            return NO_NAME;
        }
        NameTable table = table(kind);
        if (!table.contains(name) && !createByFirstUse(table, kind, name)) {
            return REJECTED;
        }
        table.markUsed(name, currentFrame.getAsLong());
        return table.backingOf(name);
    }

    public int nameOfBacking(ObjectKind kind, int backing) {
        return backing == NO_NAME ? NO_NAME : table(kind).nameOf(backing).orElse(NO_NAME);
    }

    private boolean createByFirstUse(NameTable table, ObjectKind kind, int name) {
        if (profile == ContextProfile.CORE || !CREATED_BY_FIRST_USE.contains(kind)) {
            errors.raise(GL_INVALID_OPERATION);
            return false;
        }
        int backing = provider.create(kind, NO_PARAMETER);
        if (table.claim(name, backing)) {
            return true;
        }
        provider.dispose(kind, backing);
        errors.raise(GL_OUT_OF_MEMORY);
        return false;
    }

    private NameTable table(ObjectKind kind) {
        return currentNames.get().table(kind);
    }
}
