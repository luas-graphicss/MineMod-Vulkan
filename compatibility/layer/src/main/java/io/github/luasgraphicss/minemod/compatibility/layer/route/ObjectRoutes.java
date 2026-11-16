package io.github.luasgraphicss.minemod.compatibility.layer.route;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import io.github.luasgraphicss.minemod.compatibility.layer.memory.NativeInts;
import io.github.luasgraphicss.minemod.compatibility.layer.memory.NativeMemory;
import io.github.luasgraphicss.minemod.compatibility.translation.names.ObjectNames;
import io.github.luasgraphicss.minemod.compatibility.translation.provider.ProviderCalls;
import io.github.luasgraphicss.minemod.compatibility.translation.query.QueryCalls;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

final class ObjectRoutes {
    private static final int CREATES_PROGRAM = 0;
    private static final long NOT_AN_OBJECT = 0;
    private static final Map<String, ObjectKind> GENERATED = new HashMap<>();
    private static final Map<String, ObjectKind> DELETED = new HashMap<>();
    private static final Map<String, ObjectKind> QUERIED = new HashMap<>();

    static {
        family(ObjectKind.TEXTURE, "Textures", "Texture");
        family(ObjectKind.BUFFER, "Buffers", "Buffer");
        family(ObjectKind.VERTEX_ARRAY, "VertexArrays", "VertexArray");
        family(ObjectKind.RENDERBUFFER, "Renderbuffers", "Renderbuffer");
        family(ObjectKind.SAMPLER, "Samplers", "Sampler");
        family(ObjectKind.QUERY, "Queries", "Query");
        family(ObjectKind.FRAMEBUFFER, "Framebuffers", "Framebuffer");
        QUERIED.put("glIsShader", ObjectKind.SHADER_OR_PROGRAM);
        QUERIED.put("glIsProgram", ObjectKind.SHADER_OR_PROGRAM);
    }

    private final ObjectNames names;
    private final QueryCalls queries;
    private final ListRoutes lists;
    private final ProviderCalls provider;
    private final NativeMemory memory;

    ObjectRoutes(ObjectNames names, QueryCalls queries, ListRoutes lists, ProviderCalls provider, NativeMemory memory) {
        this.names = names;
        this.queries = queries;
        this.lists = lists;
        this.provider = provider;
        this.memory = memory;
    }

    long call(String coreFunction, String function, long[] words) {
        Optional<Long> listAnswer = lists.call(coreFunction, words);
        if (listAnswer.isPresent()) {
            return listAnswer.get();
        }
        ObjectKind generated = GENERATED.get(coreFunction);
        if (generated != null) {
            NativeInts.write(memory, words[1], names.generate(generated, (int) words[0]));
            return 0;
        }
        ObjectKind deleted = DELETED.get(coreFunction);
        if (deleted != null) {
            delete(deleted, NativeInts.read(memory, words[1], (int) words[0]));
            return 0;
        }
        ObjectKind queried = QUERIED.get(coreFunction);
        if (queried != null) {
            return isObject(queried, function, (int) words[0]);
        }
        return shaderCall(coreFunction, function, words);
    }

    private long shaderCall(String coreFunction, String function, long[] words) {
        switch (coreFunction) {
            case "glCreateShader":
            case "glCreateShaderObjectARB":
                return names.create(ObjectKind.SHADER_OR_PROGRAM, (int) words[0]);
            case "glCreateProgram":
            case "glCreateProgramObjectARB":
                return names.create(ObjectKind.SHADER_OR_PROGRAM, CREATES_PROGRAM);
            case "glDeleteShader":
            case "glDeleteProgram":
            case "glDeleteObjectARB":
                names.delete(ObjectKind.SHADER_OR_PROGRAM, new int[] {(int) words[0]});
                return 0;
            case "glGetHandleARB":
                return names.nameOfBacking(ObjectKind.SHADER_OR_PROGRAM, (int) provider.invoke(function, words));
            case "glGetAttachedShaders":
            case "glGetAttachedObjectsARB":
                attachedShaders(function, words);
                return 0;
            case "glAreTexturesResident":
            case "glPrioritizeTextures":
                return callWithTextures(function, words);
            default:
                throw new IllegalArgumentException(function + " is not routed as an object call");
        }
    }

    private void delete(ObjectKind kind, int[] deleted) {
        if (kind == ObjectKind.QUERY) {
            queries.delete(deleted);
        } else {
            names.delete(kind, deleted);
        }
    }

    private long isObject(ObjectKind kind, String function, int name) {
        if (!names.isLive(kind, name)) {
            return NOT_AN_OBJECT;
        }
        return provider.invoke(function, new long[] {names.backingForUse(kind, name)});
    }

    private void attachedShaders(String function, long[] words) {
        int program = names.backingForUse(ObjectKind.SHADER_OR_PROGRAM, (int) words[0]);
        if (program == ObjectNames.REJECTED) {
            return;
        }
        int capacity = (int) words[1];
        long shaders = words[3];
        for (int index = 0; index < capacity; index++) {
            memory.writeInt(shaders + (long) index * Integer.BYTES, 0);
        }
        provider.invoke(function, new long[] {program, capacity, words[2], shaders});
        int[] attached = NativeInts.read(memory, shaders, capacity);
        for (int index = 0; index < attached.length; index++) {
            attached[index] = names.nameOfBacking(ObjectKind.SHADER_OR_PROGRAM, attached[index]);
        }
        NativeInts.write(memory, shaders, attached);
    }

    private long callWithTextures(String function, long[] words) {
        int count = (int) words[0];
        int[] textures = NativeInts.read(memory, words[1], count);
        long backings = memory.allocate(Math.max(textures.length, 1) * Integer.BYTES);
        try {
            for (int index = 0; index < textures.length; index++) {
                int backing = names.backingForUse(ObjectKind.TEXTURE, textures[index]);
                if (backing == ObjectNames.REJECTED) {
                    return NOT_AN_OBJECT;
                }
                memory.writeInt(backings + (long) index * Integer.BYTES, backing);
            }
            return provider.invoke(function, new long[] {count, backings, words[2]});
        } finally {
            memory.free(backings);
        }
    }

    private static void family(ObjectKind kind, String plural, String singular) {
        GENERATED.put("glGen" + plural, kind);
        QUERIED.put("glIs" + singular, kind);
        if (kind != ObjectKind.FRAMEBUFFER) {
            DELETED.put("glDelete" + plural, kind);
        }
    }
}
