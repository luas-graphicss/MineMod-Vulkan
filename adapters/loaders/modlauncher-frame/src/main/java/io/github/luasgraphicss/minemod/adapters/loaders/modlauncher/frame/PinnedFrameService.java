package io.github.luasgraphicss.minemod.adapters.loaders.modlauncher.frame;

import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import io.github.luasgraphicss.minemod.adapters.frame.glfw.GlfwFrameOwner;
import io.github.luasgraphicss.minemod.adapters.loaders.launch.apply.ClassNodeApplications;
import io.github.luasgraphicss.minemod.adapters.loaders.launch.plan.FramePlan;
import io.github.luasgraphicss.minemod.adapters.loaders.launch.plan.PinnedPlan;
import io.github.luasgraphicss.minemod.adapters.presentation.glfw.GlfwPresentationHooks;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class PinnedFrameService implements ITransformationService {
    private static final String NAME = "minemod-vulkan";

    private final ClassNodeApplications applications;

    public PinnedFrameService() {
        this(applicationsOf(PinnedFrameService.class.getClassLoader()));
    }

    PinnedFrameService(ClassNodeApplications applications) {
        this.applications = applications;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(IEnvironment environment) {
        GlfwFrameOwner.installFrom(PinnedFrameService.class.getClassLoader());
    }

    @Override
    public void beginScanning(IEnvironment environment) {}

    @Override
    public void onLoad(IEnvironment environment, Set<String> otherServices) {}

    @Override
    @SuppressWarnings("rawtypes")
    public List<ITransformer> transformers() {
        List<ITransformer> transformers = new ArrayList<>();
        for (String className : applications.rewrittenClasses()) {
            transformers.add(new PinnedFrameTransformer(applications, className));
        }
        return transformers;
    }

    private static ClassNodeApplications applicationsOf(ClassLoader adapter) {
        return FramePlan.of(adapter).classNodesAfter(PinnedPlan.of(adapter), GlfwPresentationHooks.INTERNAL_NAME);
    }
}
