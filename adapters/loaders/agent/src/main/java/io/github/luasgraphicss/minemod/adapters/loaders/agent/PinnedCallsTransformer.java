package io.github.luasgraphicss.minemod.adapters.loaders.agent;

import io.github.luasgraphicss.minemod.adapters.loaders.launch.apply.ClassFileApplication;
import io.github.luasgraphicss.minemod.adapters.loaders.launch.plan.PlanRefusedException;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.function.Consumer;

final class PinnedCallsTransformer implements ClassFileTransformer {
    private final ClassFileApplication application;
    private final Consumer<PlanRefusedException> onRefusal;

    PinnedCallsTransformer(ClassFileApplication application, Consumer<PlanRefusedException> onRefusal) {
        this.application = application;
        this.onRefusal = onRefusal;
    }

    @Override
    public byte[] transform(
            ClassLoader loader,
            String className,
            Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain,
            byte[] classFile) {
        if (className == null || !application.rewrites(className)) {
            return null;
        }
        try {
            return application.rewrite(className, classFile);
        } catch (PlanRefusedException refused) {
            onRefusal.accept(refused);
            return null;
        }
    }
}
