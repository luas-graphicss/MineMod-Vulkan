package io.github.luasgraphicss.minemod.adapters.presentation.display;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
import org.junit.jupiter.api.Test;
import org.lwjgl.LWJGLException;

class DisplayPresentationHooksTest {
    private static final List<String> REDIRECTED_CALLS = Arrays.asList(
            "org/lwjgl/opengl/Display.create(Lorg/lwjgl/opengl/PixelFormat;)V",
            "org/lwjgl/opengl/Display.create()V",
            "org/lwjgl/opengl/Display.update()V",
            "org/lwjgl/opengl/Display.setVSyncEnabled(Z)V",
            "org/lwjgl/opengl/Display.destroy()V");

    @Test
    void routesEveryCallOfTheGameToTheOwner() throws LWJGLException {
        RecordingOwner owner = new RecordingOwner(true, null);
        PresentationOwners.install(owner);
        try {
            DisplayPresentationHooks.create(null);
            DisplayPresentationHooks.create();
            DisplayPresentationHooks.setVSyncEnabled(true);
            DisplayPresentationHooks.setVSyncEnabled(false);
            DisplayPresentationHooks.update();
            DisplayPresentationHooks.destroy();
        } finally {
            PresentationOwners.release(owner);
        }

        assertEquals(
                Arrays.asList(
                        "open the LWJGL 2 display",
                        "open the LWJGL 2 display",
                        "pace 1",
                        "pace 0",
                        "present 0",
                        "close 0"),
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
                Collections.emptySet(), HookOwnerContract.findMissingHooks(DisplayPresentationHooks.class, redirects));
        assertEquals(
                DisplayPresentationHooks.class.getName().replace('.', '/'), DisplayPresentationHooks.INTERNAL_NAME);
    }
}
