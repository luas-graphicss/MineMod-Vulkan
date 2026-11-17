package io.github.luasgraphicss.minemod.adapters.configuration.storage;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.TreeMap;
import java.util.regex.Pattern;

public final class SettingsFile {
    private static final long MAX_BYTES = 64L * 1024;
    private static final Pattern STORABLE_KEY = Pattern.compile("[a-z0-9][a-z0-9.-]*");
    private static final Pattern STORABLE_VALUE = Pattern.compile("[^\\p{Cntrl}]*");

    private final Path file;

    public SettingsFile(Path file) {
        this.file = Objects.requireNonNull(file, "file");
    }

    public Map<String, String> read() throws IOException {
        Properties properties = new Properties();
        properties.load(new StringReader(String.join("\n", StagedFile.readLines(file, MAX_BYTES))));
        Map<String, String> stored = new TreeMap<>();
        for (String key : properties.stringPropertyNames()) {
            stored.put(key, properties.getProperty(key));
        }
        return stored;
    }

    public void write(Map<String, String> stored) throws IOException {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, String> entry : new TreeMap<>(stored).entrySet()) {
            if (!STORABLE_KEY.matcher(entry.getKey()).matches()
                    || !STORABLE_VALUE.matcher(entry.getValue()).matches()) {
                throw new IOException("setting " + entry.getKey() + " cannot be stored as one line of text");
            }
            lines.add(entry.getKey() + "=" + entry.getValue().replace("\\", "\\\\"));
        }
        StagedFile.replace(file, lines);
    }
}
