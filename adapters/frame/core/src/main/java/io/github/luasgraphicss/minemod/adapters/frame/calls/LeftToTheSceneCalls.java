package io.github.luasgraphicss.minemod.adapters.frame.calls;

import java.nio.FloatBuffer;

public final class LeftToTheSceneCalls {
    private LeftToTheSceneCalls() {}

    public static void glBlendEquation(int mode) {}

    public static void glCullFace(int culledFaces) {}

    public static void glPolygonMode(int faces, int mode) {}

    public static void glLogicOp(int operation) {}

    public static void glShadeModel(int mode) {}

    public static void glClearDepth(double depth) {}

    public static void glPixelTransferf(int name, float value) {}

    public static void glColorMaterial(int faces, int mode) {}

    public static void glNormal3f(float x, float y, float z) {}

    public static void glLightModel(int name, FloatBuffer values) {}

    public static void glLightModelfv(int name, FloatBuffer values) {}

    public static void glTexEnvi(int target, int name, int value) {}

    public static void glTexEnvf(int target, int name, float value) {}

    public static void glTexEnv(int target, int name, FloatBuffer values) {}

    public static void glTexEnvfv(int target, int name, FloatBuffer values) {}

    public static void glTexGeni(int coordinate, int name, int value) {}

    public static void glTexGen(int coordinate, int name, FloatBuffer values) {}

    public static void glTexGenfv(int coordinate, int name, FloatBuffer values) {}

    public static void glTexParameterf(int target, int name, float value) {}
}
