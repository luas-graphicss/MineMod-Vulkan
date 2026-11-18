package io.github.luasgraphicss.minemod.verification.journey.evidence;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record RendererReport(
        long frames, Map<String, Long> drawsByPass, Map<String, Resource> resources, Optional<String> refusedReload) {

    public static final String WORLD_PASS = "world";
    public static final String INTERFACE_PASS = "interface";
    public static final String TEXTURES = "texture";
    public static final String BUFFERS = "buffer";

    private static final String DETAIL_PREFIX = "Renderer: ";
    private static final String REFUSAL = "reload refused:";
    private static final Pattern FRAMES = Pattern.compile("frames: (\\d+)");
    private static final Pattern PASS = Pattern.compile("pass (.+): \\d+ us, (\\d+) draw\\(s\\)");
    private static final Pattern RESOURCE =
            Pattern.compile("resource (.+): peak (\\d+) byte\\(s\\) in (\\d+) object\\(s\\)");

    public RendererReport {
        drawsByPass = Map.copyOf(drawsByPass);
        resources = Map.copyOf(resources);
    }

    public record Resource(long peakBytes, long objects) {}

    public static Optional<RendererReport> parse(String text) {
        Optional<Long> frames = Optional.empty();
        Map<String, Long> drawsByPass = new LinkedHashMap<>();
        Map<String, Resource> resources = new LinkedHashMap<>();
        Optional<String> refusedReload = Optional.empty();
        List<String> lines = text.lines().map(RendererReport::withoutDecoration).toList();
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            Matcher counted = FRAMES.matcher(line);
            Matcher pass = PASS.matcher(line);
            Matcher resource = RESOURCE.matcher(line);
            if (counted.matches()) {
                frames = Optional.of(Long.parseLong(counted.group(1)));
            } else if (pass.matches()) {
                drawsByPass.merge(pass.group(1), Long.parseLong(pass.group(2)), Long::sum);
            } else if (resource.matches()) {
                resources.put(
                        resource.group(1),
                        new Resource(Long.parseLong(resource.group(2)), Long.parseLong(resource.group(3))));
            } else if (line.equals(REFUSAL)) {
                refusedReload = Optional.of(String.join("\n", lines.subList(index + 1, lines.size())).strip());
                break;
            }
        }
        Optional<String> refusal = refusedReload;
        return frames.map(measured -> new RendererReport(measured, drawsByPass, resources, refusal));
    }

    public long drawsIn(String pass) {
        return drawsByPass.getOrDefault(pass, 0L);
    }

    public long peakBytesOf(String resource) {
        return Optional.ofNullable(resources.get(resource)).map(Resource::peakBytes).orElse(0L);
    }

    private static String withoutDecoration(String line) {
        String stripped = line.strip();
        return stripped.startsWith(DETAIL_PREFIX) ? stripped.substring(DETAIL_PREFIX.length()) : stripped;
    }
}
