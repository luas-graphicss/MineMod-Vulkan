package io.github.luasgraphicss.minemod.adapters.fabric.mixins;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import org.spongepowered.asm.mixin.transformer.meta.MixinMerged;

final class AppliedMixins {
    private AppliedMixins() {}

    static Set<String> on(Class<?> target) {
        Set<String> applied = new HashSet<>();
        for (Class<?> accessor : target.getInterfaces()) {
            applied.add(accessor.getName());
        }
        for (Method method : target.getDeclaredMethods()) {
            MixinMerged merged = method.getAnnotation(MixinMerged.class);
            if (merged != null) {
                applied.add(merged.mixin());
            }
        }
        return applied;
    }
}
