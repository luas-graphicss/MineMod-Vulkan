package io.github.luasgraphicss.minemod.adapters.shaderpacks.pack;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public enum GeometryProgram {
    BASIC(null, false),
    LINE(BASIC, false),
    TEXTURED(BASIC, false),
    TEXTURED_LIT(TEXTURED, false),
    SKYBASIC(BASIC, false),
    SKYTEXTURED(TEXTURED, false),
    CLOUDS(TEXTURED, false),
    TERRAIN(TEXTURED_LIT, false),
    DAMAGEDBLOCK(TERRAIN, false),
    BLOCK(TERRAIN, false),
    BEACONBEAM(TEXTURED, false),
    ITEM(TEXTURED_LIT, false),
    ENTITIES(TEXTURED_LIT, false),
    ENTITIES_GLOWING(ENTITIES, false),
    ARMOR_GLINT(TEXTURED, false),
    SPIDEREYES(TEXTURED, false),
    HAND(TEXTURED_LIT, false),
    WATER(TERRAIN, true),
    WEATHER(TEXTURED_LIT, true),
    HAND_WATER(HAND, true);

    private static final String PREFIX = "gbuffers_";

    private final GeometryProgram fallback;
    private final boolean translucent;

    GeometryProgram(GeometryProgram fallback, boolean translucent) {
        this.fallback = fallback;
        this.translucent = translucent;
    }

    public static Optional<GeometryProgram> named(String programName) {
        for (GeometryProgram program : values()) {
            if (program.programName().equals(programName)) {
                return Optional.of(program);
            }
        }
        return Optional.empty();
    }

    public String programName() {
        return PREFIX + name().toLowerCase(Locale.ROOT);
    }

    public boolean translucent() {
        return translucent;
    }

    public ProgramStage stage() {
        return translucent ? ProgramStage.TRANSLUCENT_GEOMETRY : ProgramStage.OPAQUE_GEOMETRY;
    }

    public Optional<String> resolveAmong(Set<String> packPrograms) {
        for (GeometryProgram candidate = this; candidate != null; candidate = candidate.fallback) {
            if (packPrograms.contains(candidate.programName())) {
                return Optional.of(candidate.programName());
            }
        }
        return Optional.empty();
    }
}
