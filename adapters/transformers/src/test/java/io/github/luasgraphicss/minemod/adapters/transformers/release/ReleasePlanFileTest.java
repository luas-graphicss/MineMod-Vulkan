package io.github.luasgraphicss.minemod.adapters.transformers.release;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.adapters.transformers.callsite.CallSiteRedirect;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ReleasePlanFileTest {
    private static final String CLIENT_SHA1 = "c0b970952cdd279912da384cdbfc0c26e6c6090b";
    private static final String PLAN = String.join(
            "\n",
            "release=1.13",
            "client.sha1=" + CLIENT_SHA1,
            "points=window-create, swap-buffers",
            "point.window-create.callee=org/lwjgl/glfw/GLFW.glfwCreateWindow(IILjava/lang/CharSequence;JJ)J",
            "point.window-create.within=cfs.<init>(Lcfi;Lcfr;Lcnf$a;Ljava/lang/String;)V",
            "point.window-create.matches=1",
            "point.swap-buffers.callee=org/lwjgl/glfw/GLFW.glfwSwapBuffers(J)V",
            "point.swap-buffers.within=cfs.a(Z)V",
            "point.swap-buffers.matches=1");

    @Test
    void readsTheReleaseItsClientAndEveryPoint() throws IOException {
        ReleasePlan plan = read(PLAN);

        assertEquals("1.13", plan.release());
        assertTrue(plan.isPinnedFor(CLIENT_SHA1));
        assertFalse(plan.isPinnedFor(CLIENT_SHA1.replace('c', 'd')));
        assertEquals(2, plan.redirects().size());
        CallSiteRedirect windowCreation = plan.redirects().get(0);
        assertEquals("window-create", windowCreation.point().id());
        assertEquals(
                "cfs.<init>(Lcfi;Lcfr;Lcnf$a;Ljava/lang/String;)V",
                windowCreation.point().target().toString());
        assertEquals(
                "org/lwjgl/glfw/GLFW.glfwCreateWindow(IILjava/lang/CharSequence;JJ)J",
                windowCreation.callee().toString());
        assertEquals(1, windowCreation.point().expectedMatches());
    }

    @Test
    void refusesAPlanWithoutTheHashOfItsClient() {
        assertThrows(IllegalArgumentException.class, () -> read(PLAN.replace("client.sha1=" + CLIENT_SHA1, "")));
    }

    @Test
    void refusesAMethodThatIsNotWrittenWithItsDescriptor() {
        IllegalArgumentException refusal =
                assertThrows(IllegalArgumentException.class, () -> read(PLAN.replace("cfs.a(Z)V", "cfs.a")));

        assertTrue(refusal.getMessage().contains("point.swap-buffers.within"), refusal.getMessage());
    }

    @Test
    void refusesAPointThatIsListedButNotDescribed() {
        assertThrows(
                IllegalArgumentException.class,
                () -> read(PLAN.replace("points=window-create, swap-buffers", "points=window-create, vsync")));
    }

    @Test
    void refusesMatchesThatAreNotANumber() {
        assertThrows(
                IllegalArgumentException.class,
                () -> read(PLAN.replace("point.swap-buffers.matches=1", "point.swap-buffers.matches=once")));
    }

    private static ReleasePlan read(String content) throws IOException {
        return ReleasePlanFile.read(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
    }
}
