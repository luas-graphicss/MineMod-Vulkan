package io.github.luasgraphicss.minemod.adapters.transformers.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.adapters.transformers.application.PointApplication;
import io.github.luasgraphicss.minemod.adapters.transformers.callsite.CallSiteRedirect;
import io.github.luasgraphicss.minemod.adapters.transformers.fixtures.FixtureClasses;
import io.github.luasgraphicss.minemod.adapters.transformers.fixtures.GameWindow;
import io.github.luasgraphicss.minemod.adapters.transformers.fixtures.RewrittenClassLoader;
import io.github.luasgraphicss.minemod.adapters.transformers.fixtures.WindowHooks;
import io.github.luasgraphicss.minemod.adapters.transformers.fixtures.WindowLibrary;
import io.github.luasgraphicss.minemod.adapters.transformers.point.TransformPoint;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CallSiteRewriterTest {
    private static final CallSiteRedirect CREATE_IN_OPEN = new CallSiteRedirect(
            TransformPoint.hook("window-create", FixtureClasses.OPEN, 1), FixtureClasses.CREATE_WINDOW);
    private static final CallSiteRedirect SWAP_IN_FLIP = new CallSiteRedirect(
            TransformPoint.hook("swap-buffers", FixtureClasses.FLIP_TWICE, 2), FixtureClasses.SWAP_BUFFERS);

    @BeforeEach
    void forgetEarlierCalls() {
        WindowLibrary.forgetSwaps();
        WindowHooks.forgetFrames();
    }

    @Test
    void sendsARedirectedCallToTheHookOwner() throws ReflectiveOperationException {
        Object window = rewrittenWindow(Collections.singletonList(CREATE_IN_OPEN));

        assertEquals(WindowHooks.HOOKED_WINDOW, window.getClass().getMethod("open").invoke(window));
    }

    @Test
    void leavesTheSameCallAloneOutsideTheDeclaredMethod() throws ReflectiveOperationException {
        Object window = rewrittenWindow(Collections.singletonList(CREATE_IN_OPEN));

        assertEquals(WindowLibrary.LIBRARY_WINDOW, window.getClass().getMethod("reopen").invoke(window));
    }

    @Test
    void redirectsEverySiteOfTheCallWithinTheMethod() throws ReflectiveOperationException {
        Object window = rewrittenWindow(Collections.singletonList(SWAP_IN_FLIP));

        window.getClass().getMethod("flipTwice").invoke(window);

        assertEquals(2, WindowHooks.presentedFrames());
        assertEquals(0, WindowLibrary.swaps());
    }

    @Test
    void countsTheSitesOfEveryPoint() {
        List<PointApplication> applications = rewriterOf(Arrays.asList(CREATE_IN_OPEN, SWAP_IN_FLIP))
                .rewrite(FixtureClasses.GAME_WINDOW, FixtureClasses.classFileOf(GameWindow.class))
                .applications();

        assertEquals(Arrays.asList("window-create=1", "swap-buffers=2"), describe(applications));
    }

    @Test
    void reportsNoMatchWhenTheMethodDoesNotCallTheLibrary() {
        CallSiteRedirect swapInOpen = new CallSiteRedirect(
                TransformPoint.hook("swap-buffers", FixtureClasses.OPEN, 1), FixtureClasses.SWAP_BUFFERS);

        List<PointApplication> applications = rewriterOf(Collections.singletonList(swapInOpen))
                .rewrite(FixtureClasses.GAME_WINDOW, FixtureClasses.classFileOf(GameWindow.class))
                .applications();

        assertEquals(Collections.singletonList("swap-buffers=0"), describe(applications));
    }

    @Test
    void refusesTheClassFileOfAnotherClass() {
        CallSiteRewriter rewriter = rewriterOf(Collections.singletonList(CREATE_IN_OPEN));

        IllegalArgumentException refusal = assertThrows(
                IllegalArgumentException.class,
                () -> rewriter.rewrite(FixtureClasses.GAME_WINDOW, FixtureClasses.classFileOf(WindowHooks.class)));

        assertTrue(refusal.getMessage().contains(FixtureClasses.WINDOW_HOOKS), refusal.getMessage());
    }

    @Test
    void refusesAClassWithoutRedirects() {
        CallSiteRewriter rewriter = rewriterOf(Collections.singletonList(CREATE_IN_OPEN));

        assertThrows(
                IllegalArgumentException.class,
                () -> rewriter.rewrite(FixtureClasses.WINDOW_HOOKS, FixtureClasses.classFileOf(WindowHooks.class)));
    }

    @Test
    void refusesAHookOwnerThatIsNotAnInternalName() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new CallSiteRewriter(Collections.singletonList(CREATE_IN_OPEN), "game.window.Hooks"));
    }

    private static Object rewrittenWindow(List<CallSiteRedirect> redirects) throws ReflectiveOperationException {
        byte[] rewritten = rewriterOf(redirects)
                .rewrite(FixtureClasses.GAME_WINDOW, FixtureClasses.classFileOf(GameWindow.class))
                .classFile();
        return new RewrittenClassLoader(GameWindow.class, rewritten)
                .loadClass(GameWindow.class.getName())
                .getConstructor()
                .newInstance();
    }

    private static CallSiteRewriter rewriterOf(List<CallSiteRedirect> redirects) {
        return new CallSiteRewriter(redirects, FixtureClasses.WINDOW_HOOKS);
    }

    private static List<String> describe(List<PointApplication> applications) {
        String[] described = new String[applications.size()];
        for (int index = 0; index < described.length; index++) {
            described[index] = applications.get(index).pointId() + "=" + applications.get(index).matches();
        }
        return Arrays.asList(described);
    }
}
