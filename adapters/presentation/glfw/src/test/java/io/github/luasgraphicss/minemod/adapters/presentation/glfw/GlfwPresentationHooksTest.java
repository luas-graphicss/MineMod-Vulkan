package io.github.luasgraphicss.minemod.adapters.presentation.glfw;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.luasgraphicss.minemod.adapters.presentation.fixtures.RecordingOwner;
import io.github.luasgraphicss.minemod.adapters.presentation.owner.PresentationOwners;
import io.github.luasgraphicss.minemod.adapters.transformers.callsite.CallSiteRedirect;
import io.github.luasgraphicss.minemod.adapters.transformers.callsite.HookOwnerContract;
import io.github.luasgraphicss.minemod.adapters.transformers.point.MethodTarget;
import io.github.luasgraphicss.minemod.adapters.transformers.point.TransformPoint;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GlfwPresentationHooksTest {
    private static final List<String> REDIRECTED_CALLS = Arrays.asList(
            "org/lwjgl/glfw/GLFW.glfwCreateWindow(IILjava/lang/CharSequence;JJ)J",
            "org/lwjgl/glfw/GLFW.glfwMakeContextCurrent(J)V",
            "org/lwjgl/opengl/GL.createCapabilities()Lorg/lwjgl/opengl/GLCapabilities;",
            "org/lwjgl/glfw/GLFW.glfwSwapBuffers(J)V",
            "org/lwjgl/glfw/GLFW.glfwSwapInterval(I)V",
            "org/lwjgl/glfw/GLFW.glfwDestroyWindow(J)V");
    private final RecordingOwner owner = new RecordingOwner(false, null);

    @BeforeEach
    void installTheOwner() {
        PresentationOwners.install(owner);
    }

    @AfterEach
    void releaseTheOwner() {
        PresentationOwners.release(owner);
    }

    @Test
    void routesEveryCallOfTheGameToTheOwner() {
        long window = GlfwPresentationHooks.glfwCreateWindow(854, 480, "Minecraft", 0L, 0L);
        GlfwPresentationHooks.glfwMakeContextCurrent(window);
        assertNull(GlfwPresentationHooks.createCapabilities());
        GlfwPresentationHooks.glfwSwapInterval(1);
        GlfwPresentationHooks.glfwSwapBuffers(window);
        GlfwPresentationHooks.glfwDestroyWindow(window);

        assertEquals(RecordingOwner.WINDOW, window);
        assertEquals(
                Arrays.asList(
                        "open window \"Minecraft\" 854x480",
                        "attach 81",
                        "capabilities",
                        "pace 1",
                        "present 81",
                        "close 81"),
                owner.steps());
    }

    @Test
    void declaresAHookForEveryRedirectedCall() {
        List<CallSiteRedirect> redirects = new ArrayList<>();
        for (String call : REDIRECTED_CALLS) {
            TransformPoint point = TransformPoint.hook(call, MethodTarget.parse("a.b()V"), 1);
            redirects.add(new CallSiteRedirect(point, MethodTarget.parse(call)));
        }

        assertEquals(
                Collections.emptySet(), HookOwnerContract.findMissingHooks(GlfwPresentationHooks.class, redirects));
        assertEquals(
                GlfwPresentationHooks.class.getName().replace('.', '/'), GlfwPresentationHooks.INTERNAL_NAME);
    }
}
