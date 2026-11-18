package io.github.luasgraphicss.minemod.journey.inspection;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

final class LoadedLibraries {
    private static final Path MAPPINGS = Path.of("/proc/self/maps");
    private static final Pattern SHARED_OBJECT = Pattern.compile(".*\\.so(\\.[0-9]+)*");
    private static final String DELETED_SUFFIX = " (deleted)";
    private static final char PATH_SEPARATOR = '/';

    private LoadedLibraries() {}

    static List<String> namesOrNull() {
        if (!Files.isReadable(MAPPINGS)) {
            return null;
        }
        try (Stream<String> mappings = Files.lines(MAPPINGS)) {
            return mappings.filter(mapping -> mapping.indexOf(PATH_SEPARATOR) >= 0)
                    .map(LoadedLibraries::fileNameOf)
                    .filter(name -> SHARED_OBJECT.matcher(name).matches())
                    .distinct()
                    .sorted()
                    .toList();
        } catch (IOException unreadable) {
            throw new UncheckedIOException("the mappings of the process cannot be read", unreadable);
        }
    }

    private static String fileNameOf(String mapping) {
        String name = mapping.substring(mapping.lastIndexOf(PATH_SEPARATOR) + 1);
        return name.endsWith(DELETED_SUFFIX) ? name.substring(0, name.length() - DELETED_SUFFIX.length()) : name;
    }
}
