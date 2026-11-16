package io.github.luasgraphicss.minemod.bridge;

import io.github.luasgraphicss.minemod.contract.abi.AbiException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import org.junit.jupiter.api.Assumptions;

final class CoreShaderSamples {
    static final String TINTED_VERTEX = "#version 150\n"
            + "\n"
            + "in vec3 Position;\n"
            + "in vec2 UV0;\n"
            + "in vec4 Color;\n"
            + "\n"
            + "uniform mat4 ModelViewMat;\n"
            + "uniform mat4 ProjMat;\n"
            + "\n"
            + "out vec2 surfaceCoordinate;\n"
            + "out vec4 tint;\n"
            + "\n"
            + "void main() {\n"
            + "    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);\n"
            + "    surfaceCoordinate = UV0;\n"
            + "    tint = Color;\n"
            + "}\n";

    static final String TINTED_FRAGMENT = "#version 150\n"
            + "\n"
            + "uniform sampler2D Sampler0;\n"
            + "uniform vec4 ColorModulator;\n"
            + "\n"
            + "in vec2 surfaceCoordinate;\n"
            + "in vec4 tint;\n"
            + "\n"
            + "out vec4 fragColor;\n"
            + "\n"
            + "void main() {\n"
            + "    vec4 color = texture(Sampler0, surfaceCoordinate) * tint * ColorModulator;\n"
            + "    if (color.a < 0.1) {\n"
            + "        discard;\n"
            + "    }\n"
            + "    fragColor = color;\n"
            + "}\n";

    static final int SPIRV_MAGIC = 0x07230203;

    private static final String[] COMPILER_NAMES = {"glslangValidator", "glslang"};

    private CoreShaderSamples() {}

    static CoreShaderSource tinted(String name) {
        return new CoreShaderSource(name, TINTED_VERTEX, TINTED_FRAGMENT, Collections.<String, String>emptyMap());
    }

    static CoreShaderSource broken(String name) {
        return new CoreShaderSource(
                name, TINTED_VERTEX, TINTED_FRAGMENT + "void broken() { undeclared = 1; }\n",
                Collections.<String, String>emptyMap());
    }

    static NativeShaderService openService(Path scratchDirectory) {
        Assumptions.assumeTrue(LoadedBridge.BRIDGE.negotiation() != null && NativeShaderService.isAvailable());
        try {
            return NativeShaderService.open(ShaderServiceSetup.of(installedCompiler(), scratchDirectory));
        } catch (AbiException unavailable) {
            return Assumptions.abort("the shader service does not start here: " + unavailable.getMessage());
        }
    }

    private static Path installedCompiler() {
        String searchPath = System.getenv("PATH");
        for (String directory : searchPath == null ? new String[0] : searchPath.split(File.pathSeparator)) {
            for (String name : COMPILER_NAMES) {
                Path candidate = Paths.get(directory, name);
                if (!directory.isEmpty() && Files.isExecutable(candidate)) {
                    return candidate;
                }
            }
        }
        return Assumptions.abort("no glslang compiler is installed");
    }
}
