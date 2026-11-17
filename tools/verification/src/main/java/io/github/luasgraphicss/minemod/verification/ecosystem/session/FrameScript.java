package io.github.luasgraphicss.minemod.verification.ecosystem.session;

import io.github.luasgraphicss.minemod.adapters.events.contract.LoaderEventContract;
import io.github.luasgraphicss.minemod.adapters.events.frame.FrameEvents;
import io.github.luasgraphicss.minemod.adapters.events.frame.FramePoint;
import io.github.luasgraphicss.minemod.adapters.events.frame.FramePoint.Scope;
import io.github.luasgraphicss.minemod.compatibility.bridges.session.BridgeSession;
import io.github.luasgraphicss.minemod.verification.ecosystem.mods.FixtureBridge;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

final class FrameScript {
    static final List<String> HUD_ELEMENTS = List.of("crosshair", "hotbar", "health");
    static final String BLOCK_OUTLINE = "block outline";
    static final String HAND = "hand";

    private static final List<String> TERRAIN_PASS = List.of(FixtureBridge.TERRAIN_MESHES);
    private static final List<String> TRANSLUCENT_PASS =
            List.of(FixtureBridge.FLUID_SURFACES, FixtureBridge.CABLE_MESHES);

    private static final Pattern FRAME_STAMP = Pattern.compile("@f([0-9]+)");

    private final LoaderEventContract contract;
    private final SessionServices services;
    private final BridgeSession bridges;
    private final Optional<ShaderFrames> shaders;

    FrameScript(
            LoaderEventContract contract, SessionServices services, BridgeSession bridges, Optional<ShaderFrames> shaders) {
        this.contract = contract;
        this.services = services;
        this.bridges = bridges;
        this.shaders = shaders;
    }

    List<String> render() {
        List<String> nativeDraws = new ArrayList<>();
        SessionClock clock = services.clock;
        clock.beginFrame();
        long[] frameArguments = {clock.world(), clock.frame()};
        FrameEvents frame = new FrameEvents(contract, services.bus, EnumSet.allOf(Scope.class));

        frame.announce(FramePoint.FRAME_START);
        shaders.ifPresent(pipeline -> pipeline.beginFrame(clock.frame()));
        frame.announce(FramePoint.AFTER_SETUP);
        frame.announce(FramePoint.AFTER_SKY);
        services.failures.addAll(bridges.servePass(TERRAIN_PASS, frameArguments));
        shaders.ifPresent(ShaderFrames::drawTerrain);
        frame.announce(FramePoint.AFTER_SOLID_TERRAIN);
        frame.announce(FramePoint.AFTER_CUTOUT_TERRAIN);
        frame.announce(FramePoint.BEFORE_ENTITIES);
        frame.announce(FramePoint.AFTER_ENTITIES);
        frame.announce(FramePoint.AFTER_BLOCK_ENTITIES);
        if (frame.permits(FramePoint.BEFORE_BLOCK_OUTLINE)) {
            nativeDraws.add(BLOCK_OUTLINE);
        }
        shaders.ifPresent(ShaderFrames::drawTranslucents);
        frame.announce(FramePoint.AFTER_TRANSLUCENT_TERRAIN);
        services.failures.addAll(bridges.servePass(TRANSLUCENT_PASS, frameArguments));
        frame.announce(FramePoint.AFTER_PARTICLES);
        frame.announce(FramePoint.AFTER_WEATHER);
        frame.announce(FramePoint.WORLD_LAST);
        if (frame.permits(FramePoint.BEFORE_HAND)) {
            nativeDraws.add(HAND);
        }
        shaders.ifPresent(pipeline -> checkPresented(pipeline.finishFrame(), clock.frame()));
        if (frame.permits(FramePoint.BEFORE_HUD)) {
            frame.announce(FramePoint.HUD_ELEMENTS);
            for (String element : HUD_ELEMENTS) {
                if (frame.permitsElement(FramePoint.HUD_ELEMENTS, element)) {
                    nativeDraws.add(element);
                }
                frame.completeElement(FramePoint.HUD_ELEMENTS, element);
            }
            frame.announce(FramePoint.AFTER_HUD);
        }
        clock.present();
        frame.announce(FramePoint.FRAME_END);
        List<FramePoint> missed = frame.close();
        if (!missed.isEmpty()) {
            services.failures.add("frame " + clock.frame() + " never posted " + missed);
        }
        return nativeDraws;
    }

    private void checkPresented(String presented, int frame) {
        List<String> frames = FRAME_STAMP.matcher(presented).results().map(stamp -> stamp.group(1)).distinct().toList();
        if (!frames.equals(List.of(Integer.toString(frame))) || presented.contains("empty")) {
            services.failures.add("frame " + frame + " of the shader pack presented " + presented);
        }
    }
}
