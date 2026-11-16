package io.github.luasgraphicss.minemod.compatibility.translation.query;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

enum NameBinding {
    TEXTURE_BINDING_1D(0x8068, ObjectKind.TEXTURE),
    TEXTURE_BINDING_2D(0x8069, ObjectKind.TEXTURE),
    TEXTURE_BINDING_3D(0x806A, ObjectKind.TEXTURE),
    TEXTURE_BINDING_RECTANGLE(0x84F6, ObjectKind.TEXTURE),
    TEXTURE_BINDING_CUBE_MAP(0x8514, ObjectKind.TEXTURE),
    TEXTURE_BINDING_1D_ARRAY(0x8C1C, ObjectKind.TEXTURE),
    TEXTURE_BINDING_2D_ARRAY(0x8C1D, ObjectKind.TEXTURE),
    TEXTURE_BINDING_2D_MULTISAMPLE(0x9104, ObjectKind.TEXTURE),
    ARRAY_BUFFER_BINDING(0x8894, ObjectKind.BUFFER),
    ELEMENT_ARRAY_BUFFER_BINDING(0x8895, ObjectKind.BUFFER),
    PIXEL_PACK_BUFFER_BINDING(0x88ED, ObjectKind.BUFFER),
    PIXEL_UNPACK_BUFFER_BINDING(0x88EF, ObjectKind.BUFFER),
    UNIFORM_BUFFER_BINDING(0x8A28, ObjectKind.BUFFER),
    COPY_READ_BUFFER_BINDING(0x8F36, ObjectKind.BUFFER),
    COPY_WRITE_BUFFER_BINDING(0x8F37, ObjectKind.BUFFER),
    CURRENT_PROGRAM(0x8B8D, ObjectKind.SHADER_OR_PROGRAM),
    VERTEX_ARRAY_BINDING(0x85B5, ObjectKind.VERTEX_ARRAY),
    RENDERBUFFER_BINDING(0x8CA7, ObjectKind.RENDERBUFFER),
    SAMPLER_BINDING(0x8919, ObjectKind.SAMPLER);

    private static final Map<Integer, ObjectKind> KINDS_BY_PARAMETER = new HashMap<>();

    static {
        for (NameBinding binding : values()) {
            KINDS_BY_PARAMETER.put(binding.parameter, binding.kind);
        }
    }

    private final int parameter;
    private final ObjectKind kind;

    NameBinding(int parameter, ObjectKind kind) {
        this.parameter = parameter;
        this.kind = kind;
    }

    static Optional<ObjectKind> kindBoundBy(int parameter) {
        return Optional.ofNullable(KINDS_BY_PARAMETER.get(parameter));
    }
}
