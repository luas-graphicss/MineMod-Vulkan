package io.github.luasgraphicss.minemod.adapters.transformers.callsite;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import org.objectweb.asm.Type;

public final class HookOwnerContract {
    private HookOwnerContract() {}

    public static Set<String> findMissingHooks(Class<?> hookOwner, Collection<CallSiteRedirect> redirects) {
        Set<String> declared = new TreeSet<>();
        for (Method method : hookOwner.getDeclaredMethods()) {
            if (Modifier.isStatic(method.getModifiers()) && Modifier.isPublic(method.getModifiers())) {
                declared.add(method.getName() + Type.getMethodDescriptor(method));
            }
        }
        Set<String> missing = new TreeSet<>();
        for (CallSiteRedirect redirect : redirects) {
            String hook = redirect.callee().name() + redirect.callee().descriptor();
            if (!declared.contains(hook)) {
                missing.add(hook);
            }
        }
        return missing;
    }
}
