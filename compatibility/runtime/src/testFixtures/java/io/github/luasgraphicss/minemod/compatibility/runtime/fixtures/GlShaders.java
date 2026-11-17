package io.github.luasgraphicss.minemod.compatibility.runtime.fixtures;

public final class GlShaders {
    private static final int GL_INTERLEAVED_ATTRIBS = 0x8C8C;

    private GlShaders() {
    }

    public static int compile(GlScene gl, int type, String source) {
        int shader = (int) gl.call("glCreateShader", type);
        try (NativeBlock text = NativeBlock.ofText(source); NativeBlock strings = new NativeBlock(Long.BYTES)) {
            strings.setLong(0, text.address());
            gl.call("glShaderSource", shader, 1, strings.address(), 0);
        }
        gl.call("glCompileShader", shader);
        return shader;
    }

    public static int shaderParameter(GlScene gl, int shader, int parameter) {
        try (NativeBlock answer = new NativeBlock(Integer.BYTES)) {
            gl.call("glGetShaderiv", shader, parameter, answer.address());
            return answer.intAt(0);
        }
    }

    public static int programParameter(GlScene gl, int program, int parameter) {
        try (NativeBlock answer = new NativeBlock(Integer.BYTES)) {
            gl.call("glGetProgramiv", program, parameter, answer.address());
            return answer.intAt(0);
        }
    }

    public static void bindAttribute(GlScene gl, int program, int index, String attribute) {
        try (NativeBlock name = NativeBlock.ofText(attribute)) {
            gl.call("glBindAttribLocation", program, index, name.address());
        }
    }

    public static void captureInterleaved(GlScene gl, int program, String varying) {
        try (NativeBlock name = NativeBlock.ofText(varying); NativeBlock names = new NativeBlock(Long.BYTES)) {
            names.setLong(0, name.address());
            gl.call("glTransformFeedbackVaryings", program, 1, names.address(), GL_INTERLEAVED_ATTRIBS);
        }
    }
}
