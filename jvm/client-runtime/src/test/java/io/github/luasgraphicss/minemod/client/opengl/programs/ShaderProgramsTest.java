package io.github.luasgraphicss.minemod.client.opengl.programs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class ShaderProgramsTest {
    private static final String VERTEX_STAGE = "#version 150\n"
            + "uniform mat4 ModelViewMat;\n"
            + "uniform sampler2D Sampler2;\n"
            + "in vec3 Position;\n"
            + "void main() {\n"
            + "    gl_Position = ModelViewMat * vec4(Position, 1.0);\n"
            + "}\n";
    private static final String FRAGMENT_STAGE = "#version 150\n"
            + "uniform sampler2D Sampler0;\n"
            + "uniform vec4 ColorModulator;\n"
            + "uniform float FogStart;\n"
            + "out vec4 fragColor;\n"
            + "void main() {\n"
            + "    fragColor = texture(Sampler0, vec2(0.0)) * ColorModulator;\n"
            + "}\n";

    private final ShaderPrograms programs = new ShaderPrograms();
    private final int program = linkedProgram();

    @Test
    void findsAUniformThatAStageUses() {
        assertNotEquals(ShaderPrograms.NO_LOCATION, programs.uniformLocation(program, "ColorModulator"));
        assertNotEquals(ShaderPrograms.NO_LOCATION, programs.uniformLocation(program, "ModelViewMat"));
        assertNotEquals(ShaderPrograms.NO_LOCATION, programs.uniformLocation(program, "Sampler0"));
    }

    @Test
    void missesAUniformThatIsOnlyDeclared() {
        assertEquals(ShaderPrograms.NO_LOCATION, programs.uniformLocation(program, "FogStart"));
        assertEquals(ShaderPrograms.NO_LOCATION, programs.uniformLocation(program, "Sampler2"));
    }

    @Test
    void missesANameNoStageMentions() {
        assertEquals(ShaderPrograms.NO_LOCATION, programs.uniformLocation(program, "Sampler1"));
    }

    @Test
    void missesANameThatIsOnlyPartOfAnother() {
        assertEquals(ShaderPrograms.NO_LOCATION, programs.uniformLocation(program, "Sampler"));
        assertEquals(ShaderPrograms.NO_LOCATION, programs.uniformLocation(program, "Color"));
    }

    @Test
    void keepsOneLocationForEachUniform() {
        int tint = programs.uniformLocation(program, "ColorModulator");
        int sampler = programs.uniformLocation(program, "Sampler0");

        assertNotEquals(tint, sampler);
        assertEquals(tint, programs.uniformLocation(program, "ColorModulator"));
    }

    @Test
    void forgetsADeletedProgram() {
        programs.deleteProgram(program);

        assertEquals(ShaderPrograms.NO_LOCATION, programs.uniformLocation(program, "ColorModulator"));
    }

    @Test
    void missesTheUniformsOfADeletedStage() {
        int stage = programs.createShader();
        programs.source(stage, Collections.singletonList(FRAGMENT_STAGE));
        int emptied = programs.createProgram();
        programs.attach(emptied, stage);
        programs.deleteShader(stage);

        assertEquals(ShaderPrograms.NO_LOCATION, programs.uniformLocation(emptied, "ColorModulator"));
    }

    private int linkedProgram() {
        int vertex = programs.createShader();
        programs.source(vertex, Collections.singletonList(VERTEX_STAGE));
        int fragment = programs.createShader();
        programs.source(fragment, Arrays.asList(FRAGMENT_STAGE.substring(0, 40), FRAGMENT_STAGE.substring(40)));
        int linked = programs.createProgram();
        programs.attach(linked, vertex);
        programs.attach(linked, fragment);
        return linked;
    }
}
