package io.github.luasgraphicss.minemod.client.compatibility.provider;

import io.github.luasgraphicss.minemod.client.compatibility.ModOpenGlUnavailable;
import io.github.luasgraphicss.minemod.compatibility.provider.artifact.ArtifactDigest;
import io.github.luasgraphicss.minemod.compatibility.provider.artifact.ProviderPin;
import io.github.luasgraphicss.minemod.compatibility.runtime.provider.ProviderBundle;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.regex.Pattern;

public final class ProviderManifest {
    public static final String FILE_NAME = "provider.properties";
    private static final long MAX_BYTES = 64 * 1024;
    private static final String MESA = "mesa";
    private static final String ENTRY = "entry";
    private static final String DEPENDENCIES = "dependencies";
    private static final String DIGEST_PREFIX = "sha256.";
    private static final String SETTING_PREFIX = "driver.";
    private static final Pattern LIBRARY_NAME = Pattern.compile("[A-Za-z0-9_][A-Za-z0-9_.+-]*");
    private static final Pattern SETTING_NAME = Pattern.compile("[A-Z][A-Z0-9_]*");
    private static final Pattern BLANKS = Pattern.compile("\\s+");

    private final Path file;
    private final Properties entries;

    private ProviderManifest(Path file, Properties entries) {
        this.file = file;
        this.entries = entries;
    }

    public static ProviderBundle read(Path directory) {
        Path file = directory.resolve(FILE_NAME);
        return new ProviderManifest(file, load(file)).bundleIn(directory);
    }

    private ProviderBundle bundleIn(Path directory) {
        String entry = libraryNamed(required(ENTRY));
        List<String> dependencies = dependenciesOf(entry);
        Map<String, String> settings = driverSettings();
        requireNothingElse(entry, dependencies);
        Map<String, ArtifactDigest> digests = new LinkedHashMap<>();
        for (String dependency : dependencies) {
            digests.put(dependency, digestOf(dependency));
        }
        ProviderPin pin = new ProviderPin(required(MESA), digestOf(entry));
        return new ProviderBundle(directory, entry, pin, digests, settings);
    }

    private List<String> dependenciesOf(String entry) {
        List<String> dependencies = new ArrayList<>();
        String listed = entries.getProperty(DEPENDENCIES, "").trim();
        if (listed.isEmpty()) {
            return dependencies;
        }
        for (String dependency : BLANKS.split(listed)) {
            if (dependency.equals(entry) || dependencies.contains(dependency)) {
                throw rejected("lists " + dependency + " twice");
            }
            dependencies.add(libraryNamed(dependency));
        }
        return dependencies;
    }

    private Map<String, String> driverSettings() {
        Map<String, String> settings = new TreeMap<>();
        for (String key : entries.stringPropertyNames()) {
            if (!key.startsWith(SETTING_PREFIX)) {
                continue;
            }
            String setting = key.substring(SETTING_PREFIX.length());
            if (!SETTING_NAME.matcher(setting).matches()) {
                throw rejected("sets " + setting + ", which is not the name of a driver setting");
            }
            settings.put(setting, entries.getProperty(key));
        }
        return settings;
    }

    private void requireNothingElse(String entry, List<String> dependencies) {
        for (String key : entries.stringPropertyNames()) {
            boolean isDigest = key.startsWith(DIGEST_PREFIX);
            String digested = isDigest ? key.substring(DIGEST_PREFIX.length()) : "";
            boolean isKnown = key.equals(MESA)
                    || key.equals(ENTRY)
                    || key.equals(DEPENDENCIES)
                    || key.startsWith(SETTING_PREFIX)
                    || (isDigest && (digested.equals(entry) || dependencies.contains(digested)));
            if (!isKnown) {
                throw rejected("holds " + key + ", which describes nothing the provider loads");
            }
        }
    }

    private ArtifactDigest digestOf(String library) {
        String digest = required(DIGEST_PREFIX + library);
        try {
            return ArtifactDigest.ofHex(digest);
        } catch (IllegalArgumentException malformed) {
            throw new ModOpenGlUnavailable("reading the digest of " + library + " in " + file, malformed);
        }
    }

    private String libraryNamed(String name) {
        if (!LIBRARY_NAME.matcher(name).matches()) {
            throw rejected("names " + name + ", which is not the file name of a library");
        }
        return name;
    }

    private String required(String key) {
        String value = entries.getProperty(key, "").trim();
        if (value.isEmpty()) {
            throw rejected("holds no " + key);
        }
        return value;
    }

    private ModOpenGlUnavailable rejected(String defect) {
        return new ModOpenGlUnavailable("the provider manifest " + file + " " + defect);
    }

    private static Properties load(Path file) {
        try {
            if (Files.size(file) > MAX_BYTES) {
                throw new ModOpenGlUnavailable(
                        "the provider manifest " + file + " is larger than " + MAX_BYTES + " bytes");
            }
            Properties entries = new Properties();
            entries.load(new StringReader(new String(Files.readAllBytes(file), StandardCharsets.UTF_8)));
            return entries;
        } catch (IOException | IllegalArgumentException failure) {
            throw new ModOpenGlUnavailable("reading the provider manifest " + file, failure);
        }
    }
}
