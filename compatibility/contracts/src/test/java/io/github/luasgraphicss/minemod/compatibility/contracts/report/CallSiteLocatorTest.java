package io.github.luasgraphicss.minemod.compatibility.contracts.report;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CallSiteLocatorTest {
    private static final List<String> INFRASTRUCTURE =
            Arrays.asList("io.github.luasgraphicss.minemod.", "org.lwjgl.", "java.");

    @Test
    void attributesTheCallToTheFirstFrameOutsideTheInfrastructure() {
        CallSiteLocator locator = new CallSiteLocator(
                INFRASTRUCTURE, className -> Optional.of("file:/home/pedro/.minecraft/mods/example.jar"));

        CallSite site = locator.locate(new StackTraceElement[] {
            frame("org.lwjgl.opengl.GL11", "glBegin"),
            frame("com.example.mod.Overlay", "render"),
            frame("com.example.mod.Events", "onRender"),
        });

        assertEquals(new CallSite("com.example.mod.Overlay", "render", "example.jar"), site);
    }

    @Test
    void reportsAnUnknownCallSiteWhenEveryFrameIsInfrastructure() {
        CallSiteLocator locator = new CallSiteLocator(INFRASTRUCTURE, className -> Optional.empty());

        CallSite site = locator.locate(new StackTraceElement[] {
            frame("org.lwjgl.opengl.GL11", "glBegin"), frame("java.lang.Thread", "run"),
        });

        assertEquals(CallSite.unknown(), site);
    }

    @Test
    void labelsTheOriginAsUnknownWhenTheClassCannotBeLocated() {
        CallSiteLocator locator = new CallSiteLocator(INFRASTRUCTURE, className -> Optional.empty());

        CallSite site = locator.locate(new StackTraceElement[] {frame("com.example.mod.Overlay", "render")});

        assertEquals(new CallSite("com.example.mod.Overlay", "render", OriginLabel.UNKNOWN), site);
    }

    private static StackTraceElement frame(String className, String methodName) {
        return new StackTraceElement(className, methodName, null, -1);
    }
}
