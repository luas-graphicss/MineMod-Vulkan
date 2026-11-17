package io.github.luasgraphicss.minemod.compatibility.layer.sharing;

import io.github.luasgraphicss.minemod.compatibility.interop.negotiation.ShareableResource;
import io.github.luasgraphicss.minemod.compatibility.interop.negotiation.SharingRequest;
import io.github.luasgraphicss.minemod.compatibility.interop.platform.HandlePlatform;
import io.github.luasgraphicss.minemod.compatibility.layer.memory.ScratchInts;
import io.github.luasgraphicss.minemod.compatibility.state.error.ErrorLedger;
import io.github.luasgraphicss.minemod.compatibility.translation.provider.ProviderCalls;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class ProviderMemoryImports {
    private static final long[] NO_ARGUMENTS = new long[0];
    private static final int GL_NO_ERROR = 0;
    private static final int GL_TEXTURE_2D = 0x0DE1;
    private static final int GL_ARRAY_BUFFER = 0x8892;
    private static final int ONE = 1;
    private static final long NO_OFFSET = 0;
    private final Map<Integer, Imported> imports = new HashMap<>();
    private final ProviderCalls provider;
    private final ScratchInts scratch;
    private final ErrorLedger errors;
    private final HandleImports handles;

    public ProviderMemoryImports(ProviderCalls provider, ScratchInts scratch, ErrorLedger errors) {
        this(provider, scratch, errors, HandlePlatform.current());
    }

    public ProviderMemoryImports(
            ProviderCalls provider, ScratchInts scratch, ErrorLedger errors, HandlePlatform platform) {
        this.provider = Objects.requireNonNull(provider, "provider");
        this.scratch = Objects.requireNonNull(scratch, "scratch");
        this.errors = Objects.requireNonNull(errors, "errors");
        this.handles = HandleImports.on(platform);
    }

    public int importMemory(SharingRequest request, long allocationSize, long handle) {
        int earlierError = errors.take();
        try {
            return importChecked(request, allocationSize, handle);
        } finally {
            if (earlierError != GL_NO_ERROR) {
                errors.raise(earlierError);
            }
        }
    }

    private int importChecked(SharingRequest request, long allocationSize, long handle) {
        int memoryObject = generate("glCreateMemoryObjectsEXT");
        handles.memory(provider, memoryObject, allocationSize, handle);
        boolean isBuffer = request.resource() == ShareableResource.BUFFER;
        int object = unusedName(isBuffer ? "glGenBuffers" : "glGenTextures", isBuffer ? "glDeleteBuffers" : "glDeleteTextures");
        if (isBuffer) {
            provider.invoke("glBindBuffer", new long[] {GL_ARRAY_BUFFER, object});
            provider.invoke("glBufferStorageMemEXT", new long[] {GL_ARRAY_BUFFER, request.width(), memoryObject, NO_OFFSET});
        } else {
            provider.invoke("glBindTexture", new long[] {GL_TEXTURE_2D, object});
            provider.invoke("glTexStorageMem2DEXT", new long[] {
                GL_TEXTURE_2D,
                request.mipLevels(),
                SharedFormat.ofVulkanFormat(request.format()).internalFormat(),
                request.width(),
                request.height(),
                memoryObject,
                NO_OFFSET
            });
        }
        Imported imported = new Imported(memoryObject, isBuffer);
        int error = (int) provider.invoke("glGetError", NO_ARGUMENTS);
        if (error != GL_NO_ERROR) {
            delete(object, imported);
            throw new IllegalStateException(
                    "the provider refused to import " + request + " with error 0x" + Integer.toHexString(error));
        }
        imports.put(object, imported);
        return object;
    }

    public void release(int object) {
        Imported imported = imports.remove(object);
        if (imported == null) {
            throw new IllegalArgumentException("provider object " + object + " was not imported here");
        }
        delete(object, imported);
    }

    public int liveCount() {
        return imports.size();
    }

    private void delete(int object, Imported imported) {
        provider.invoke(imported.isBuffer ? "glDeleteBuffers" : "glDeleteTextures", new long[] {ONE, scratch.holding(object)});
        provider.invoke("glDeleteMemoryObjectsEXT", new long[] {ONE, scratch.holding(imported.memoryObject)});
    }

    private int unusedName(String generator, String deleter) {
        int name = generate(generator);
        if (!imports.containsKey(name)) {
            return name;
        }
        int unused = unusedName(generator, deleter);
        provider.invoke(deleter, new long[] {ONE, scratch.holding(name)});
        return unused;
    }

    private int generate(String generator) {
        provider.invoke(generator, new long[] {ONE, scratch.address()});
        return scratch.first();
    }

    private static final class Imported {
        private final int memoryObject;
        private final boolean isBuffer;

        private Imported(int memoryObject, boolean isBuffer) {
            this.memoryObject = memoryObject;
            this.isBuffer = isBuffer;
        }
    }
}
