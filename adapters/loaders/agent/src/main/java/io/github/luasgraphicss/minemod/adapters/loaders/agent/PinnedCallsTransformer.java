package io.github.luasgraphicss.minemod.adapters.loaders.agent;

import io.github.luasgraphicss.minemod.adapters.loaders.launch.apply.ClassFileApplications;
import io.github.luasgraphicss.minemod.adapters.loaders.launch.plan.PlanRefusedException;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.function.Consumer;

final class PinnedCallsTransformer implements ClassFileTransformer {
    private final ClassFileApplications applications;
    private final Consumer<PlanRefusedException> onRefusal;

    PinnedCallsTransformer(ClassFileApplications applications, Consumer<PlanRefusedException> onRefusal) {
        this.applications = applications;
        this.onRefusal = onRefusal;
    }

    @Override
    public byte[] transform(
            ClassLoader loader,
            String className,
            Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain,
            byte[] classFile) {
        if (className == null || !applications.rewrites(className)) {
            return null;
        }
        try {
            return applications.rewrite(className, classFile);
        } catch (PlanRefusedException refused) {
            onRefusal.accept(refused);
            return null;
        }
    }
}
