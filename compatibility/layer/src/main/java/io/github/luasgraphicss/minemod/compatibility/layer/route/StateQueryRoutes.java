package io.github.luasgraphicss.minemod.compatibility.layer.route;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import io.github.luasgraphicss.minemod.compatibility.layer.capability.ExposedExtensions;
import io.github.luasgraphicss.minemod.compatibility.layer.memory.NativeMemory;
import io.github.luasgraphicss.minemod.compatibility.translation.names.ObjectNames;
import io.github.luasgraphicss.minemod.compatibility.translation.provider.ProviderCalls;
import io.github.luasgraphicss.minemod.compatibility.translation.query.BindingQueries;
import java.util.OptionalInt;

final class StateQueryRoutes {
    private static final String INTEGER = "glGetIntegerv";
    private static final String INDEXED_INTEGER = "glGetIntegeri_v";
    private static final String INDEXED_LONG = "glGetInteger64i_v";
    private static final String DOUBLE_ATTRIBUTE = "glGetVertexAttribdv";
    private static final String FLOAT_ATTRIBUTE = "glGetVertexAttribfv";
    private static final int GL_NUM_EXTENSIONS = 0x821D;
    private static final int GL_VERTEX_ATTRIB_ARRAY_BUFFER_BINDING = 0x889F;
    private final BindingQueries bindings;
    private final ObjectNames names;
    private final ExposedExtensions exposed;
    private final ProviderCalls provider;
    private final NativeMemory memory;

    StateQueryRoutes(
            BindingQueries bindings,
            ObjectNames names,
            ExposedExtensions exposed,
            ProviderCalls provider,
            NativeMemory memory) {
        this.bindings = bindings;
        this.names = names;
        this.exposed = exposed;
        this.provider = provider;
        this.memory = memory;
    }

    void call(String coreFunction, String function, long[] words) {
        if (coreFunction.equals(INTEGER)) {
            integer(function, words);
        } else if (coreFunction.equals(INDEXED_INTEGER) || coreFunction.equals(INDEXED_LONG)) {
            provider.invoke(function, words);
            showVisibleName((int) words[0], words[2]);
        } else {
            vertexAttribute(coreFunction, function, words);
        }
    }

    private void integer(String function, long[] words) {
        int parameter = (int) words[0];
        long answer = words[1];
        if (parameter == GL_NUM_EXTENSIONS) {
            memory.writeInt(answer, exposed.count());
            return;
        }
        OptionalInt answeredHere = bindings.answeredHere(parameter);
        if (answeredHere.isPresent()) {
            memory.writeInt(answer, answeredHere.getAsInt());
            return;
        }
        provider.invoke(function, words);
        showVisibleName(parameter, answer);
    }

    private void showVisibleName(int parameter, long answer) {
        int providerValue = memory.readInt(answer);
        int visible = bindings.toVisibleValue(parameter, providerValue);
        if (visible != providerValue) {
            memory.writeInt(answer, visible);
        }
    }

    private void vertexAttribute(String coreFunction, String function, long[] words) {
        provider.invoke(function, words);
        if (words[1] != GL_VERTEX_ATTRIB_ARRAY_BUFFER_BINDING) {
            return;
        }
        long answer = words[2];
        if (coreFunction.equals(DOUBLE_ATTRIBUTE)) {
            double backing = Double.longBitsToDouble(memory.readLong(answer));
            memory.writeLong(answer, Double.doubleToRawLongBits(visibleBuffer((int) backing)));
        } else if (coreFunction.equals(FLOAT_ATTRIBUTE)) {
            float backing = Float.intBitsToFloat(memory.readInt(answer));
            memory.writeInt(answer, Float.floatToRawIntBits(visibleBuffer((int) backing)));
        } else {
            memory.writeInt(answer, visibleBuffer(memory.readInt(answer)));
        }
    }

    private int visibleBuffer(int backing) {
        return names.nameOfBacking(ObjectKind.BUFFER, backing);
    }
}
