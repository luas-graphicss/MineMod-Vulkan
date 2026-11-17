package io.github.luasgraphicss.minemod.adapters.transformers.fixtures;

public final class RewrittenClassLoader extends ClassLoader {
    private final String rewrittenName;
    private final byte[] classFile;

    public RewrittenClassLoader(Class<?> original, byte[] classFile) {
        super(original.getClassLoader());
        this.rewrittenName = original.getName();
        this.classFile = classFile;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (!name.equals(rewrittenName)) {
            return super.loadClass(name, resolve);
        }
        synchronized (getClassLoadingLock(name)) {
            Class<?> loaded = findLoadedClass(name);
            return loaded != null ? loaded : defineClass(name, classFile, 0, classFile.length);
        }
    }
}
