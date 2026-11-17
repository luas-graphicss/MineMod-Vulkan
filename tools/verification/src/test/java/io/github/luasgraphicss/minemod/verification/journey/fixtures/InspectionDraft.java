package io.github.luasgraphicss.minemod.verification.journey.fixtures;

import io.github.luasgraphicss.minemod.verification.journey.record.Inspection;
import java.util.ArrayList;
import java.util.List;

public final class InspectionDraft {
    public static final String BUILT_IN = "shaders: built-in programs";
    public static final String ONE_REPLACED = "shaders: 1 replaced by resource packs";
    public static final String REFUSED =
            "shaders: reload refused, previous programs kept (position_tex_color.fsh:11: syntax error)";

    private static final List<String> NATIVE_LIBRARIES =
            List.of("libc.so.6", "libglfw.so", "liblwjgl.so", "libminemod_jni.so", "libvulkan.so.1", "libvulkan_lvp.so");
    private static final List<String> OPENGL_LIBRARIES =
            List.of("libGL.so.1", "libGLX.so.0", "libc.so.6", "libglfw.so", "liblwjgl.so", "liblwjgl_opengl.so");

    private final String name;
    private String dimension = "minecraft:overworld";
    private boolean hasNativeRenderer = true;
    private long frames = 120;
    private long worldDraws = 37;
    private long bufferBytes = 2048;
    private long generation = 1;
    private long retiringResources;
    private long leftOutDraws;
    private String shaders = BUILT_IN;
    private String refusal = "";
    private List<String> libraries = NATIVE_LIBRARIES;

    private InspectionDraft(String name) {
        this.name = name;
    }

    public static InspectionDraft named(String name) {
        return new InspectionDraft(name);
    }

    public InspectionDraft in(String visitedDimension) {
        dimension = visitedDimension;
        return this;
    }

    public InspectionDraft withTheRendererOfTheGame() {
        hasNativeRenderer = false;
        libraries = OPENGL_LIBRARIES;
        return this;
    }

    public InspectionDraft withFrames(long measured) {
        frames = measured;
        return this;
    }

    public InspectionDraft withWorldDraws(long draws) {
        worldDraws = draws;
        return this;
    }

    public InspectionDraft withBufferBytes(long bytes) {
        bufferBytes = bytes;
        return this;
    }

    public InspectionDraft atGeneration(long reached) {
        generation = reached;
        return this;
    }

    public InspectionDraft retiring(long resources) {
        retiringResources = resources;
        return this;
    }

    public InspectionDraft leavingOut(long draws) {
        leftOutDraws = draws;
        return this;
    }

    public InspectionDraft withShaders(String line) {
        shaders = line;
        return this;
    }

    public InspectionDraft refusing(String reason) {
        shaders = REFUSED;
        refusal = reason;
        return this;
    }

    public InspectionDraft mapping(List<String> mapped) {
        libraries = mapped;
        return this;
    }

    public Inspection build() {
        return new Inspection(
                name,
                dimension,
                dimension == null ? -1 : 2400,
                debugLines(),
                hasNativeRenderer ? rendererReport() : null,
                libraries,
                List.of("vanilla"));
    }

    private List<String> debugLines() {
        List<String> lines = new ArrayList<>(List.of("Java: 21.0.4", "Mem: 21% 512/2048MB", "", "CPU: 8x Example"));
        if (hasNativeRenderer) {
            lines.addAll(List.of(
                    "",
                    "MineMod Vulkan",
                    "frame 0.90 ms mean, 2.10 ms worst over " + frames,
                    "textures 12.5 MiB peak, staging 0.8 MiB peak",
                    "9 pipelines peak, " + retiringResources + " resources retiring, generation " + generation,
                    shaders,
                    "draws left out of the scene: " + leftOutDraws));
        }
        return lines;
    }

    private String rendererReport() {
        String report = "-- MineMod Vulkan --\nDetails:\n\tRenderer: frames: " + frames
                + "\n\t\tframe time: mean 900 us, worst 2100 us"
                + "\n\t\tpass write textures: 40 us, 2 draw(s)"
                + "\n\t\tpass world: 410 us, " + worldDraws + " draw(s)"
                + "\n\t\tpass interface: 95 us, 6 draw(s)"
                + "\n\t\tresource buffer: peak " + bufferBytes + " byte(s) in 3 object(s)"
                + "\n\t\tresource texture: peak 13107200 byte(s) in 14 object(s)"
                + "\n\t\tqueue section uploads: peak 4 of 64";
        return refusal.isEmpty() ? report : report + "\n\t\treload refused:\n\t\t" + refusal;
    }
}
