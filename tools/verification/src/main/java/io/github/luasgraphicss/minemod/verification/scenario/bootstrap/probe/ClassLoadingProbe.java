package io.github.luasgraphicss.minemod.verification.scenario.bootstrap.probe;

public final class ClassLoadingProbe {
    private ClassLoadingProbe() {}

    public static void main(String[] classNames) throws ClassNotFoundException {
        for (String className : classNames) {
            Class.forName(className.replace('/', '.'), false, ClassLoadingProbe.class.getClassLoader());
        }
    }
}
