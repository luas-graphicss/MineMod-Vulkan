package io.github.luasgraphicss.minemod.adapters.events.frame;

import java.util.Optional;

public enum FramePoint {
    FRAME_START(Scope.FRAME),
    AFTER_SETUP(Scope.WORLD),
    AFTER_SKY(Scope.WORLD),
    AFTER_SOLID_TERRAIN(Scope.WORLD),
    AFTER_CUTOUT_TERRAIN(Scope.WORLD),
    BEFORE_ENTITIES(Scope.WORLD),
    AFTER_ENTITIES(Scope.WORLD),
    AFTER_BLOCK_ENTITIES(Scope.WORLD),
    BEFORE_BLOCK_OUTLINE(Scope.WORLD),
    AFTER_TRANSLUCENT_TERRAIN(Scope.WORLD),
    AFTER_PARTICLES(Scope.WORLD),
    AFTER_WEATHER(Scope.WORLD),
    WORLD_LAST(Scope.WORLD),
    BEFORE_HAND(Scope.WORLD),
    BEFORE_HUD(Scope.HUD),
    HUD_ELEMENTS(Scope.HUD, BEFORE_HUD),
    AFTER_HUD(Scope.HUD, BEFORE_HUD),
    BEFORE_SCREEN(Scope.SCREEN),
    AFTER_SCREEN(Scope.SCREEN, BEFORE_SCREEN),
    FRAME_END(Scope.FRAME);

    public enum Scope {
        FRAME,
        WORLD,
        HUD,
        SCREEN
    }

    private final Scope scope;
    private final FramePoint guard;

    FramePoint(Scope scope) {
        this(scope, null);
    }

    FramePoint(Scope scope, FramePoint guard) {
        this.scope = scope;
        this.guard = guard;
    }

    public Scope scope() {
        return scope;
    }

    public Optional<FramePoint> guard() {
        return Optional.ofNullable(guard);
    }
}
