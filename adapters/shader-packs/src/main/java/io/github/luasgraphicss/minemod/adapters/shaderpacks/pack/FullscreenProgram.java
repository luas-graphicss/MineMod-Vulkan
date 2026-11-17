package io.github.luasgraphicss.minemod.adapters.shaderpacks.pack;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FullscreenProgram implements Comparable<FullscreenProgram> {
    private static final Pattern NUMBERED = Pattern.compile("(shadowcomp|prepare|deferred|composite)([1-9][0-9]?)?");
    private static final String FINAL = "final";

    private final String name;
    private final ProgramStage stage;
    private final int position;

    private FullscreenProgram(String name, ProgramStage stage, int position) {
        this.name = name;
        this.stage = stage;
        this.position = position;
    }

    public static Optional<FullscreenProgram> named(String programName) {
        if (programName.equals(FINAL)) {
            return Optional.of(new FullscreenProgram(programName, ProgramStage.FINAL, 0));
        }
        Matcher numbered = NUMBERED.matcher(programName);
        if (!numbered.matches()) {
            return Optional.empty();
        }
        int position = numbered.group(2) == null ? 0 : Integer.parseInt(numbered.group(2));
        return Optional.of(new FullscreenProgram(programName, stageNamed(numbered.group(1)), position));
    }

    public String name() {
        return name;
    }

    public ProgramStage stage() {
        return stage;
    }

    @Override
    public int compareTo(FullscreenProgram other) {
        int byStage = stage.compareTo(other.stage);
        return byStage != 0 ? byStage : Integer.compare(position, other.position);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FullscreenProgram && name.equals(((FullscreenProgram) other).name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    private static ProgramStage stageNamed(String family) {
        switch (family) {
            case "shadowcomp":
                return ProgramStage.SHADOW_COMPOSITE;
            case "prepare":
                return ProgramStage.PREPARE;
            case "deferred":
                return ProgramStage.DEFERRED;
            default:
                return ProgramStage.COMPOSITE;
        }
    }
}
