package io.github.luasgraphicss.minemod.verification.scenario.synchronous;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import io.github.luasgraphicss.minemod.verification.scenario.simulation.GlDriver;
import io.github.luasgraphicss.minemod.verification.scenario.simulation.SimulatedProvider;
import java.util.ArrayList;
import java.util.List;

final class ProviderAnswersScript {
    private static final long GL_TRIANGLES = 0x0004;
    private static final long GL_INVALID_ENUM = 0x0500;
    private static final long GL_QUERY_RESULT = 0x8866;
    private static final long GL_QUERY_RESULT_AVAILABLE = 0x8867;
    private static final long GL_SAMPLES_PASSED = 0x8914;
    private static final long GL_COMPILE_STATUS = 0x8B81;
    private static final long GL_LINK_STATUS = 0x8B82;
    private static final long GL_INFO_LOG_LENGTH = 0x8B84;
    private static final long SUCCEEDED = 1;
    private static final long FAILED = 0;
    private static final long COMPILE_LOG_LENGTH = 57;
    private static final long LINK_LOG_LENGTH = 31;
    private static final long PENDING = 0;
    private static final long AVAILABLE = 1;
    private static final long SAMPLES = 1234;
    private static final long UNKNOWN_PROGRAM = 404;

    static final List<Long> ANSWERS = List.of(
            SUCCEEDED,
            FAILED,
            COMPILE_LOG_LENGTH,
            FAILED,
            LINK_LOG_LENGTH,
            PENDING,
            AVAILABLE,
            SAMPLES,
            GL_INVALID_ENUM,
            (long) SimulatedProvider.NO_ERROR);

    private ProviderAnswersScript() {}

    static void script(SimulatedProvider provider) {
        provider.answer("glGetShaderiv", SUCCEEDED, FAILED, COMPILE_LOG_LENGTH);
        provider.answer("glGetProgramiv", FAILED, LINK_LOG_LENGTH);
        provider.answer("glGetQueryObjectuiv", PENDING, AVAILABLE, SAMPLES);
    }

    static List<Long> play(GlDriver gl) {
        List<Long> answers = new ArrayList<>();
        int[] objects = gl.generate(ObjectKind.SHADER_OR_PROGRAM, 3);
        int valid = objects[0];
        int invalid = objects[1];
        int program = objects[2];
        gl.call("glCompileShader", valid);
        answers.add(gl.call("glGetShaderiv", valid, GL_COMPILE_STATUS, 0));
        gl.call("glCompileShader", invalid);
        answers.add(gl.call("glGetShaderiv", invalid, GL_COMPILE_STATUS, 0));
        answers.add(gl.call("glGetShaderiv", invalid, GL_INFO_LOG_LENGTH, 0));
        gl.call("glAttachShader", program, valid);
        gl.call("glAttachShader", program, invalid);
        gl.call("glLinkProgram", program);
        answers.add(gl.call("glGetProgramiv", program, GL_LINK_STATUS, 0));
        answers.add(gl.call("glGetProgramiv", program, GL_INFO_LOG_LENGTH, 0));
        int query = gl.generate(ObjectKind.QUERY, 1)[0];
        gl.call("glBeginQuery", GL_SAMPLES_PASSED, query);
        gl.call("glDrawArrays", GL_TRIANGLES, 0, 3);
        gl.call("glEndQuery", GL_SAMPLES_PASSED);
        answers.add(gl.call("glGetQueryObjectuiv", query, GL_QUERY_RESULT_AVAILABLE, 0));
        answers.add(gl.call("glGetQueryObjectuiv", query, GL_QUERY_RESULT_AVAILABLE, 0));
        answers.add(gl.call("glGetQueryObjectuiv", query, GL_QUERY_RESULT, 0));
        gl.call("glEnable", SimulatedProvider.INVALID_CAPABILITY);
        gl.call("glUseProgram", UNKNOWN_PROGRAM);
        answers.add((long) gl.takeError());
        answers.add((long) gl.takeError());
        return answers;
    }
}
