package io.github.luasgraphicss.minemod.adapters.loaders.launchwrapper;

import io.github.luasgraphicss.minemod.adapters.loaders.launch.apply.ClassFileApplication;
import io.github.luasgraphicss.minemod.adapters.loaders.launch.plan.PinnedPlan;
import io.github.luasgraphicss.minemod.adapters.presentation.display.DisplayPresentationHooks;
import net.minecraft.launchwrapper.IClassTransformer;

public final class PinnedCallsTransformer implements IClassTransformer {
    private final ClassFileApplication application;

    public PinnedCallsTransformer() {
        this(applicationOf(PinnedCallsTransformer.class));
    }

    PinnedCallsTransformer(ClassFileApplication application) {
        this.application = application;
    }

    static ClassFileApplication applicationOf(Class<?> adapterClass) {
        return new ClassFileApplication(
                PinnedPlan.of(adapterClass.getClassLoader()), DisplayPresentationHooks.INTERNAL_NAME);
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] classFile) {
        if (classFile == null) {
            return null;
        }
        String internalName = name.replace('.', '/');
        return application.rewrites(internalName) ? application.rewrite(internalName, classFile) : classFile;
    }
}
