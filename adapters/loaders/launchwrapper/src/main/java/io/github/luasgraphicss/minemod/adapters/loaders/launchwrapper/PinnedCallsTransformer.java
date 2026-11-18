package io.github.luasgraphicss.minemod.adapters.loaders.launchwrapper;

import io.github.luasgraphicss.minemod.adapters.loaders.launch.apply.ClassFileApplications;
import io.github.luasgraphicss.minemod.adapters.loaders.launch.plan.FramePlan;
import io.github.luasgraphicss.minemod.adapters.loaders.launch.plan.PinnedPlan;
import io.github.luasgraphicss.minemod.adapters.presentation.display.DisplayPresentationHooks;
import net.minecraft.launchwrapper.IClassTransformer;

public final class PinnedCallsTransformer implements IClassTransformer {
    private final ClassFileApplications applications;

    public PinnedCallsTransformer() {
        this(applicationsOf(PinnedCallsTransformer.class));
    }

    PinnedCallsTransformer(ClassFileApplications applications) {
        this.applications = applications;
    }

    static ClassFileApplications applicationsOf(Class<?> adapterClass) {
        ClassLoader adapter = adapterClass.getClassLoader();
        return FramePlan.of(adapter).classFilesAfter(PinnedPlan.of(adapter), DisplayPresentationHooks.INTERNAL_NAME);
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] classFile) {
        if (classFile == null) {
            return null;
        }
        String internalName = name.replace('.', '/');
        return applications.rewrites(internalName) ? applications.rewrite(internalName, classFile) : classFile;
    }
}
