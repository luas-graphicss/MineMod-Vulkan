package io.github.luasgraphicss.minemod.adapters.shaderpacks.reader;

import io.github.luasgraphicss.minemod.adapters.shaderpacks.pack.ProgramSources;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.pack.ShaderPack;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.regex.Pattern;

public final class PackReader {
    private static final int MAX_FILES = 4096;
    private static final long MAX_FILE_BYTES = 1024L * 1024;
    private static final long MAX_PROGRAM_BYTES = 4L * 1024 * 1024;
    private static final String PROPERTIES_FILE = "shaders.properties";
    private static final Pattern COMPUTE_VARIANT = Pattern.compile("_[a-z]$");

    private PackReader() {}

    public static ShaderPack read(String name, Path shadersDirectory) throws IOException {
        Path root = shadersDirectory.toAbsolutePath().normalize();
        Map<String, ProgramFiles> filesByProgram = new TreeMap<>();
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(root)) {
            int seen = 0;
            for (Path entry : entries) {
                if (++seen > MAX_FILES) {
                    throw new IOException(root + " holds more than " + MAX_FILES + " entries");
                }
                if (Files.isRegularFile(entry, LinkOption.NOFOLLOW_LINKS)) {
                    assign(entry, filesByProgram);
                }
            }
        }
        IncludeExpander expander = new IncludeExpander(root, MAX_FILE_BYTES, MAX_PROGRAM_BYTES);
        Map<String, ProgramSources> programs = new TreeMap<>();
        for (Map.Entry<String, ProgramFiles> program : filesByProgram.entrySet()) {
            ProgramFiles files = program.getValue();
            programs.put(
                    program.getKey(),
                    new ProgramSources(
                            files.vertex == null ? "" : expander.expand(files.vertex),
                            files.fragment == null ? "" : expander.expand(files.fragment),
                            files.computeShaders,
                            files.geometryShader));
        }
        return new ShaderPack(name, programs, propertiesOf(root));
    }

    private static void assign(Path file, Map<String, ProgramFiles> filesByProgram) {
        String fileName = file.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        if (dot <= 0) {
            return;
        }
        String program = fileName.substring(0, dot);
        switch (fileName.substring(dot + 1)) {
            case "vsh":
                filesOf(program, filesByProgram).vertex = file;
                break;
            case "fsh":
                filesOf(program, filesByProgram).fragment = file;
                break;
            case "gsh":
                filesOf(program, filesByProgram).geometryShader = true;
                break;
            case "csh":
                filesOf(COMPUTE_VARIANT.matcher(program).replaceFirst(""), filesByProgram).computeShaders++;
                break;
            default:
                break;
        }
    }

    private static ProgramFiles filesOf(String program, Map<String, ProgramFiles> filesByProgram) {
        return filesByProgram.computeIfAbsent(program, absent -> new ProgramFiles());
    }

    private static Map<String, String> propertiesOf(Path root) throws IOException {
        Path file = root.resolve(PROPERTIES_FILE);
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            return Collections.emptyMap();
        }
        if (Files.size(file) > MAX_FILE_BYTES) {
            throw new IOException(PROPERTIES_FILE + " is larger than " + MAX_FILE_BYTES + " bytes");
        }
        Properties loaded = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            loaded.load(reader);
        }
        Map<String, String> properties = new TreeMap<>();
        for (String key : loaded.stringPropertyNames()) {
            properties.put(key, loaded.getProperty(key));
        }
        return properties;
    }

    private static final class ProgramFiles {
        private Path vertex;
        private Path fragment;
        private boolean geometryShader;
        private int computeShaders;
    }
}
