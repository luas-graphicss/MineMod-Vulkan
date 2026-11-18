package io.github.luasgraphicss.minemod.adapters.loaders.launchwrapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.adapters.frame.session.GameFrames;
import io.github.luasgraphicss.minemod.adapters.loaders.launch.fixtures.FixtureGame;
import io.github.luasgraphicss.minemod.adapters.loaders.launch.plan.PlanRefusedException;
import io.github.luasgraphicss.minemod.adapters.presentation.display.DisplayPresentationHooks;
import io.github.luasgraphicss.minemod.adapters.presentation.fixtures.InstalledOwners;
import io.github.luasgraphicss.minemod.adapters.presentation.owner.PresentationOwners;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PinnedCallsTweakerTest {
    private final PinnedCallsTweaker tweaker = new PinnedCallsTweaker();

    @TempDir
    Path gameClasses;

    @AfterEach
    void releaseTheOwner() {
        InstalledOwners.release();
    }

    @Test
    void registersItsTransformerOnTheGameThePlanWasPinnedOn() throws IOException {
        FixtureGame.writeWindow(gameClasses, FixtureGame.windowCallingEach(FixtureGame.DISPLAY_UPDATE, FixtureGame.MATRIX_MODE));
        LaunchClassLoader classLoader = launchClassLoader();

        tweaker.injectIntoClassLoader(classLoader);

        List<IClassTransformer> transformers = classLoader.getTransformers();
        assertEquals(1, transformers.size());
        assertEquals(PinnedCallsTransformer.class.getName(), transformers.get(0).getClass().getName());
    }

    @Test
    void installsTheNativeOwnerWhereTheClassesOfTheGameFindIt() throws IOException, ClassNotFoundException {
        FixtureGame.writeWindow(gameClasses, FixtureGame.windowCallingEach(FixtureGame.DISPLAY_UPDATE, FixtureGame.MATRIX_MODE));
        LaunchClassLoader classLoader = launchClassLoader();

        tweaker.injectIntoClassLoader(classLoader);

        assertSame(PresentationOwners.class, classLoader.findClass(PresentationOwners.class.getName()));
        assertSame(DisplayPresentationHooks.class, classLoader.findClass(DisplayPresentationHooks.class.getName()));
        assertSame(GameFrames.class, classLoader.findClass(GameFrames.class.getName()));
        assertTrue(PresentationOwners.isInstalled());
        assertFalse(PresentationOwners.current().keepsOpenGlContext());
    }

    @Test
    void refusesAGameWhoseClassesAreNotThePinnedOnes() throws IOException {
        FixtureGame.writeWindow(gameClasses, FixtureGame.windowCalling(FixtureGame.DISPLAY_UPDATE, 3));
        LaunchClassLoader classLoader = launchClassLoader();

        assertThrows(PlanRefusedException.class, () -> tweaker.injectIntoClassLoader(classLoader));
        assertTrue(classLoader.getTransformers().isEmpty());
        assertFalse(PresentationOwners.isInstalled());
    }

    @Test
    void namesNoLaunchTargetOfItsOwn() {
        assertEquals("", tweaker.getLaunchTarget());
        assertEquals(0, tweaker.getLaunchArguments().length);
    }

    private LaunchClassLoader launchClassLoader() throws IOException {
        List<URL> sources = new ArrayList<>();
        sources.add(gameClasses.toUri().toURL());
        for (String entry : System.getProperty("java.class.path").split(Pattern.quote(File.pathSeparator))) {
            sources.add(new File(entry).toURI().toURL());
        }
        return new LaunchClassLoader(sources.toArray(new URL[0]));
    }
}
