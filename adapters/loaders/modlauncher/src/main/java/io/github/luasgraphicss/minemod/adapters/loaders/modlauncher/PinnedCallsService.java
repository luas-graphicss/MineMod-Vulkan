package io.github.luasgraphicss.minemod.adapters.loaders.modlauncher;

import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import io.github.luasgraphicss.minemod.adapters.loaders.launch.apply.ClassNodeApplication;
import io.github.luasgraphicss.minemod.adapters.loaders.launch.plan.PinnedPlan;
import io.github.luasgraphicss.minemod.adapters.presentation.glfw.GlfwNativeOwner;
import io.github.luasgraphicss.minemod.adapters.presentation.glfw.GlfwPresentationHooks;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class PinnedCallsService implements ITransformationService {
    private static final String NAME = "minemod-vulkan";

    private final ClassNodeApplication application;

    public PinnedCallsService() {
        this(new ClassNodeApplication(
                PinnedPlan.of(PinnedCallsService.class.getClassLoader()), GlfwPresentationHooks.INTERNAL_NAME));
    }

    PinnedCallsService(ClassNodeApplication application) {
        this.application = application;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(IEnvironment environment) {
        GlfwNativeOwner.installFrom(PinnedCallsService.class.getClassLoader());
    }

    @Override
    public void beginScanning(IEnvironment environment) {}

    @Override
    public void onLoad(IEnvironment environment, Set<String> otherServices) {}

    @Override
    @SuppressWarnings("rawtypes")
    public List<ITransformer> transformers() {
        List<ITransformer> transformers = new ArrayList<>();
        for (String className : application.rewrittenClasses()) {
            transformers.add(new PinnedClassTransformer(application, className));
        }
        return transformers;
    }
}
