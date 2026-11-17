package io.github.luasgraphicss.minemod.adapters.shaderpacks.reader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class IncludeExpander {
    private static final Pattern INCLUDE = Pattern.compile("\\s*#include\\s+\"([^\"]+)\"\\s*");
    private static final int MAX_DEPTH = 10;

    private final Path root;
    private final long maxFileBytes;
    private final long maxProgramBytes;
    private long expandedBytes;

    IncludeExpander(Path root, long maxFileBytes, long maxProgramBytes) {
        this.root = root;
        this.maxFileBytes = maxFileBytes;
        this.maxProgramBytes = maxProgramBytes;
    }

    String expand(Path file) throws IOException {
        expandedBytes = 0;
        return expand(file, 0);
    }

    private String expand(Path file, int depth) throws IOException {
        if (depth > MAX_DEPTH) {
            throw new IOException("includes nest deeper than " + MAX_DEPTH + " at " + root.relativize(file));
        }
        String text = read(file);
        StringBuilder expanded = new StringBuilder(text.length());
        for (String line : text.split("\n", -1)) {
            Matcher include = INCLUDE.matcher(line);
            if (include.matches()) {
                expanded.append(expand(resolve(file, include.group(1)), depth + 1));
            } else {
                expanded.append(line);
            }
            expanded.append('\n');
        }
        return expanded.toString();
    }

    private Path resolve(Path includer, String reference) throws IOException {
        Path target = reference.startsWith("/")
                ? root.resolve(reference.substring(1))
                : includer.getParent().resolve(reference);
        Path normalized = target.normalize();
        if (!normalized.startsWith(root)) {
            throw new IOException("include \"" + reference + "\" leaves the shader pack");
        }
        return normalized;
    }

    private String read(Path file) throws IOException {
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException(root.relativize(file) + " is not a file of the shader pack");
        }
        long size = Files.size(file);
        expandedBytes += size;
        if (size > maxFileBytes || expandedBytes > maxProgramBytes) {
            throw new IOException(root.relativize(file) + " makes the program larger than the pack reader accepts");
        }
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }
}
