package io.github.luasgraphicss.minemod.adapters.loaders.agent;

import io.github.luasgraphicss.minemod.adapters.loaders.launch.apply.ClassFileApplication;
import io.github.luasgraphicss.minemod.adapters.loaders.launch.plan.PinnedPlan;
import io.github.luasgraphicss.minemod.adapters.loaders.launch.plan.PlanRefusedException;
import io.github.luasgraphicss.minemod.adapters.presentation.glfw.GlfwPresentationHooks;
import io.github.luasgraphicss.minemod.adapters.transformers.release.ReleasePlan;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.function.Consumer;

public final class ClientBootstrapAgent {
    private static final String CLASS_PATH = "java.class.path";
    private static final int REFUSED_LAUNCH = 1;

    private ClientBootstrapAgent() {}

    public static void premain(String options, Instrumentation instrumentation) throws IOException {
        install(
                PinnedPlan.of(ClientBootstrapAgent.class.getClassLoader()),
                System.getProperty(CLASS_PATH),
                instrumentation,
                ClientBootstrapAgent::haltBecauseTheJvmIgnoresTransformerFailures);
    }

    static void install(
            ReleasePlan plan,
            String classPath,
            Instrumentation instrumentation,
            Consumer<PlanRefusedException> onRefusal)
            throws IOException {
        ClientJar client = ClientJar.onClassPath(classPath);
        client.requirePinnedBy(plan);
        ClassFileApplication application = new ClassFileApplication(plan, GlfwPresentationHooks.INTERNAL_NAME);
        try (ClientJar.Classes classes = client.openClasses()) {
            application.requireApplicableTo(classes);
        }
        instrumentation.addTransformer(new PinnedCallsTransformer(application, onRefusal));
    }

    private static void haltBecauseTheJvmIgnoresTransformerFailures(PlanRefusedException refused) {
        System.err.println("MineMod-Vulkan: " + refused.getMessage());
        Runtime.getRuntime().halt(REFUSED_LAUNCH);
    }
}
