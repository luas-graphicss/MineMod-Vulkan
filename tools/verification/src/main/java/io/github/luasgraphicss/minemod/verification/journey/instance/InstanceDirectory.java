package io.github.luasgraphicss.minemod.verification.journey.instance;

import io.github.luasgraphicss.minemod.verification.journey.plan.JourneyPlan;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Stream;

public final class InstanceDirectory {
    private static final String GAME = "game";
    private static final String OPTIONS = "options.txt";
    private static final String RESOURCE_PACKS = "resourcepacks";
    private static final String CAPTURES = "captures";
    private static final String IMAGE_EXTENSION = ".png";

    private InstanceDirectory() {}

    public static void prepare(Path directory, JourneyPlan plan, Path packs) throws IOException {
        empty(directory);
        Path game = Files.createDirectories(directory.resolve(GAME));
        Files.writeString(game.resolve(OPTIONS), optionsOf(plan));
        for (String pack : plan.packs()) {
            copy(packs.resolve(pack), game.resolve(RESOURCE_PACKS).resolve(pack));
        }
    }

    public static Path captureIn(Path directory, String name) {
        return directory.resolve(CAPTURES).resolve(name + IMAGE_EXTENSION);
    }

    private static String optionsOf(JourneyPlan plan) {
        StringBuilder options = new StringBuilder();
        new TreeMap<>(plan.options())
                .forEach((option, value) -> options.append(option).append(':').append(value).append('\n'));
        return options.toString();
    }

    private static void copy(Path source, Path target) throws IOException {
        List<Path> files;
        try (Stream<Path> walked = Files.walk(source)) {
            files = walked.toList();
        }
        for (Path file : files) {
            Path copied = target.resolve(source.relativize(file).toString());
            if (Files.isDirectory(file)) {
                Files.createDirectories(copied);
            } else {
                Files.copy(file, copied);
            }
        }
    }

    private static void empty(Path directory) throws IOException {
        if (Files.exists(directory)) {
            List<Path> contents;
            try (Stream<Path> walked = Files.walk(directory)) {
                contents = walked.sorted(Comparator.reverseOrder()).toList();
            }
            for (Path path : contents) {
                Files.delete(path);
            }
        }
        Files.createDirectories(directory);
    }
}
