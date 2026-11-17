package io.github.luasgraphicss.minemod.adapters.configuration.diagnostics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public final class DiagnosticReport {
    private static final String TITLE = "MineMod Vulkan";
    private static final String NOTHING = "none";
    private static final Pattern CONTROL_CHARACTER = Pattern.compile("\\p{Cntrl}");

    private final List<String> lines;

    private DiagnosticReport(List<String> lines) {
        this.lines = Collections.unmodifiableList(new ArrayList<>(lines));
    }

    public static Builder forTarget(String minecraftRelease, String loader) {
        return new Builder(TITLE + " on Minecraft " + printable(minecraftRelease) + ", " + printable(loader));
    }

    public List<String> lines() {
        return lines;
    }

    private static String printable(String text) {
        return CONTROL_CHARACTER.matcher(text).replaceAll("?");
    }

    public static final class Builder {
        private final List<String> lines = new ArrayList<>();

        private Builder(String heading) {
            lines.add(heading);
        }

        public Builder section(String name, Collection<String> entries) {
            List<String> printed = new ArrayList<>();
            for (String entry : entries) {
                printed.add(printable(entry));
            }
            lines.add(name + ": " + (printed.isEmpty() ? NOTHING : String.join("; ", printed)));
            return this;
        }

        public DiagnosticReport build() {
            return new DiagnosticReport(lines);
        }
    }
}
