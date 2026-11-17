package io.github.luasgraphicss.minemod.adapters.shaderpacks.targets;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SamplerNames {
    public static final String WATER_SHADOW = "watershadow";

    private static final Pattern NUMBERED = Pattern.compile("(colortex|depthtex|shadowtex|shadowcolor)([0-9]{1,2})");
    private static final String[] LEGACY_COLOR_NAMES = {
        "gcolor", "gdepth", "gnormal", "composite", "gaux1", "gaux2", "gaux3", "gaux4"
    };
    private static final Set<String> GAME_TEXTURES =
            new HashSet<>(Arrays.asList("gtexture", "texture", "tex", "lightmap", "normals", "specular", "noisetex"));
    private static final String LEGACY_DEPTH = "gdepthtex";
    private static final String LEGACY_SHADOW = "shadow";
    private static final String LEGACY_SHADOW_COLOR = "shadowcolor";

    private SamplerNames() {}

    public static boolean boundByTheGame(String sampler) {
        return GAME_TEXTURES.contains(sampler);
    }

    public static OptionalInt colorIndexOf(String name) {
        Optional<PackTarget> target = numberedTarget(name);
        if (target.isPresent()) {
            return target.get().kind() == PackTarget.Kind.COLOR
                    ? OptionalInt.of(target.get().index())
                    : OptionalInt.empty();
        }
        int legacy = Arrays.asList(LEGACY_COLOR_NAMES).indexOf(name);
        return legacy < 0 ? OptionalInt.empty() : OptionalInt.of(legacy);
    }

    public static Optional<PackTarget> targetOf(String sampler, boolean programDeclaresWaterShadow) {
        Optional<PackTarget> numbered = numberedTarget(sampler);
        if (numbered.isPresent()) {
            return numbered;
        }
        OptionalInt legacyColor = colorIndexOf(sampler);
        if (legacyColor.isPresent()) {
            return Optional.of(PackTarget.color(legacyColor.getAsInt()));
        }
        switch (sampler) {
            case LEGACY_DEPTH:
                return Optional.of(PackTarget.depth(0));
            case LEGACY_SHADOW:
                return Optional.of(PackTarget.shadowDepth(programDeclaresWaterShadow ? 1 : 0));
            case WATER_SHADOW:
                return Optional.of(PackTarget.shadowDepth(0));
            case LEGACY_SHADOW_COLOR:
                return Optional.of(PackTarget.shadowColor(0));
            default:
                return Optional.empty();
        }
    }

    private static Optional<PackTarget> numberedTarget(String name) {
        Matcher numbered = NUMBERED.matcher(name);
        if (!numbered.matches()) {
            return Optional.empty();
        }
        PackTarget.Kind kind = kindWithPrefix(numbered.group(1));
        int index = Integer.parseInt(numbered.group(2));
        if (index >= kind.count()) {
            return Optional.empty();
        }
        switch (kind) {
            case COLOR:
                return Optional.of(PackTarget.color(index));
            case DEPTH:
                return Optional.of(PackTarget.depth(index));
            case SHADOW_DEPTH:
                return Optional.of(PackTarget.shadowDepth(index));
            default:
                return Optional.of(PackTarget.shadowColor(index));
        }
    }

    private static PackTarget.Kind kindWithPrefix(String prefix) {
        switch (prefix) {
            case "colortex":
                return PackTarget.Kind.COLOR;
            case "depthtex":
                return PackTarget.Kind.DEPTH;
            case "shadowtex":
                return PackTarget.Kind.SHADOW_DEPTH;
            default:
                return PackTarget.Kind.SHADOW_COLOR;
        }
    }
}
