package io.github.luasgraphicss.minemod.adapters.loaders.modlauncher11;

import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import io.github.luasgraphicss.minemod.adapters.loaders.launch.apply.ClassNodeApplication;
import io.github.luasgraphicss.minemod.adapters.loaders.launch.plan.PinnedPlan;
import io.github.luasgraphicss.minemod.adapters.presentation.glfw.GlfwPresentationHooks;
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
    public void initialize(IEnvironment environment) {}

    @Override
    public void onLoad(IEnvironment environment, Set<String> otherServices) {}

    @Override
    public List<? extends ITransformer<?>> transformers() {
        return application.rewrittenClasses().stream()
                .map(className -> new PinnedClassTransformer(application, className))
                .toList();
    }
}
