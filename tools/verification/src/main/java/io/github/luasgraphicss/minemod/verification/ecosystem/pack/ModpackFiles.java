package io.github.luasgraphicss.minemod.verification.ecosystem.pack;

import io.github.luasgraphicss.minemod.verification.json.RepositoryJson;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Stream;

public final class ModpackFiles {
    public static final String REPOSITORY_DIRECTORY = "tests/modpacks";

    private static final String EXTENSION = ".json";
    private static final long MAX_BYTES = 256L * 1024;
    private static final int MAX_PACKS = 256;

    private ModpackFiles() {}

    public static SortedMap<String, Modpack> readFrom(Path repositoryRoot) throws IOException {
        Path directory = repositoryRoot.resolve(REPOSITORY_DIRECTORY);
        List<Path> files;
        try (Stream<Path> entries = Files.list(directory)) {
            files = entries.filter(entry -> entry.getFileName().toString().endsWith(EXTENSION))
                    .limit(MAX_PACKS + 1L)
                    .toList();
        }
        if (files.size() > MAX_PACKS) {
            throw new IOException(directory + " holds more than " + MAX_PACKS + " modpacks");
        }
        SortedMap<String, Modpack> packsByPath = new TreeMap<>();
        for (Path file : files) {
            String path = REPOSITORY_DIRECTORY + "/" + file.getFileName();
            packsByPath.put(path, RepositoryJson.read(file, Modpack.class, MAX_BYTES));
        }
        return packsByPath;
    }
}
