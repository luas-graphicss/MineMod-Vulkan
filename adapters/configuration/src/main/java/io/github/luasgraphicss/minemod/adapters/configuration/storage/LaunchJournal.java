package io.github.luasgraphicss.minemod.adapters.configuration.storage;

import io.github.luasgraphicss.minemod.adapters.configuration.recovery.LaunchPhase;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class LaunchJournal {
    private static final int KEPT_LAUNCHES = 8;
    private static final long MAX_BYTES = 4096;

    private final Path file;

    public LaunchJournal(Path file) {
        this.file = Objects.requireNonNull(file, "file");
    }

    public List<LaunchPhase> launches() throws IOException {
        List<LaunchPhase> launches = new ArrayList<>();
        for (String line : StagedFile.readLines(file, MAX_BYTES)) {
            for (LaunchPhase phase : LaunchPhase.values()) {
                if (phase.name().equals(line.trim())) {
                    launches.add(phase);
                }
            }
        }
        return launches;
    }

    public void begin() throws IOException {
        List<LaunchPhase> launches = launches();
        launches.add(LaunchPhase.STARTED);
        store(launches.subList(Math.max(0, launches.size() - KEPT_LAUNCHES), launches.size()));
    }

    public void reach(LaunchPhase phase) throws IOException {
        List<LaunchPhase> launches = launches();
        if (launches.isEmpty()) {
            throw new IllegalStateException("no launch was begun before reaching " + phase);
        }
        int current = launches.size() - 1;
        if (phase.compareTo(launches.get(current)) > 0) {
            launches.set(current, phase);
            store(launches);
        }
    }

    private void store(List<LaunchPhase> launches) throws IOException {
        List<String> lines = new ArrayList<>();
        for (LaunchPhase launch : launches) {
            lines.add(launch.name());
        }
        StagedFile.replace(file, lines);
    }
}
