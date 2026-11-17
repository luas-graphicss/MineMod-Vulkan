package io.github.luasgraphicss.minemod.adapters.loaders.knot;

import io.github.luasgraphicss.minemod.adapters.loaders.launch.apply.ClassNodeApplication;
import io.github.luasgraphicss.minemod.adapters.loaders.launch.plan.PinnedPlan;
import io.github.luasgraphicss.minemod.adapters.presentation.glfw.GlfwNativeOwner;
import io.github.luasgraphicss.minemod.adapters.presentation.glfw.GlfwPresentationHooks;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class PinnedCallsPlugin implements IMixinConfigPlugin {
    private final ClassNodeApplication application;

    public PinnedCallsPlugin() {
        this(new ClassNodeApplication(
                PinnedPlan.of(PinnedCallsPlugin.class.getClassLoader()), GlfwPresentationHooks.INTERNAL_NAME));
    }

    PinnedCallsPlugin(ClassNodeApplication application) {
        this.application = application;
    }

    @Override
    public void onLoad(String mixinPackage) {
        GlfwNativeOwner.installFrom(PinnedCallsPlugin.class.getClassLoader());
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixin) {
        application.rewrite(targetClass);
    }
}
