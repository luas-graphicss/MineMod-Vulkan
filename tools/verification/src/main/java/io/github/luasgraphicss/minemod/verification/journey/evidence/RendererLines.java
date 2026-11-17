package io.github.luasgraphicss.minemod.verification.journey.evidence;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record RendererLines(
        long framesMeasured, long retiringResources, long generation, Shaders shaders, long leftOutDraws) {
    private static final String TITLE = "MineMod Vulkan";
    private static final Pattern FRAMES = Pattern.compile("frame [0-9.]+ ms mean, [0-9.]+ ms worst over (\\d+)");
    private static final Pattern RESOURCES =
            Pattern.compile("\\d+ pipelines peak, (\\d+) resources retiring, generation (\\d+)");
    private static final Pattern REPLACED = Pattern.compile("shaders: (\\d+) replaced by resource packs");
    private static final Pattern REFUSED =
            Pattern.compile("shaders: reload refused, previous programs kept \\((.*)\\)");
    private static final String BUILT_IN = "shaders: built-in programs";
    private static final Pattern LEFT_OUT = Pattern.compile("draws left out of the scene: (\\d+)");

    public enum Programs {
        BUILT_IN,
        REPLACED,
        REFUSED
    }

    public record Shaders(Programs programs, int replaced, String refusal) {}

    public static Optional<RendererLines> parse(List<String> debugLines) {
        int title = debugLines.indexOf(TITLE);
        if (title < 0) {
            return Optional.empty();
        }
        List<String> lines = debugLines.subList(title + 1, debugLines.size());
        Optional<Matcher> frames = firstMatch(FRAMES, lines);
        Optional<Matcher> resources = firstMatch(RESOURCES, lines);
        Optional<Shaders> shaders = shadersIn(lines);
        Optional<Matcher> leftOut = firstMatch(LEFT_OUT, lines);
        if (frames.isEmpty() || resources.isEmpty() || shaders.isEmpty() || leftOut.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new RendererLines(
                Long.parseLong(frames.get().group(1)),
                Long.parseLong(resources.get().group(1)),
                Long.parseLong(resources.get().group(2)),
                shaders.get(),
                Long.parseLong(leftOut.get().group(1))));
    }

    private static Optional<Shaders> shadersIn(List<String> lines) {
        if (lines.contains(BUILT_IN)) {
            return Optional.of(new Shaders(Programs.BUILT_IN, 0, ""));
        }
        Optional<Matcher> replaced = firstMatch(REPLACED, lines);
        if (replaced.isPresent()) {
            return Optional.of(new Shaders(Programs.REPLACED, Integer.parseInt(replaced.get().group(1)), ""));
        }
        return firstMatch(REFUSED, lines).map(refused -> new Shaders(Programs.REFUSED, 0, refused.group(1)));
    }

    private static Optional<Matcher> firstMatch(Pattern pattern, List<String> lines) {
        return lines.stream().map(pattern::matcher).filter(Matcher::matches).findFirst();
    }
}
