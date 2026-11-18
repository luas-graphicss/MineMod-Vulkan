package io.github.luasgraphicss.minemod.adapters.frame.answers;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Set;

public final class BlankInstances {
    private static final String ALLOCATOR = "sun.misc.Unsafe";
    private static final String ALLOCATOR_INSTANCE = "theUnsafe";
    private static final String ALLOCATION = "allocateInstance";

    private BlankInstances() {}

    public static <T> T raising(Class<T> type, Set<String> flags) {
        try {
            T blank = type.cast(allocate(type));
            for (Field field : type.getFields()) {
                boolean isFlag = field.getType() == boolean.class && !Modifier.isStatic(field.getModifiers());
                if (isFlag && flags.contains(field.getName())) {
                    field.setAccessible(true);
                    field.setBoolean(blank, true);
                }
            }
            return blank;
        } catch (ReflectiveOperationException unavailable) {
            throw new IllegalStateException(
                    "this Java runtime cannot answer " + type.getName() + " without an OpenGL context", unavailable);
        }
    }

    private static Object allocate(Class<?> type) throws ReflectiveOperationException {
        Class<?> allocator = Class.forName(ALLOCATOR);
        Field instance = allocator.getDeclaredField(ALLOCATOR_INSTANCE);
        instance.setAccessible(true);
        return allocator.getMethod(ALLOCATION, Class.class).invoke(instance.get(null), type);
    }
}
