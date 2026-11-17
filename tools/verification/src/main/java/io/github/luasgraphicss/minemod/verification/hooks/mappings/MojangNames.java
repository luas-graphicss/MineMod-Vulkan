package io.github.luasgraphicss.minemod.verification.hooks.mappings;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MojangNames {
    private static final Pattern CLASS_LINE = Pattern.compile("(\\S+) -> (\\S+):");
    private static final Pattern METHOD_LINE =
            Pattern.compile("\\s+(?:\\d+:\\d+:)?(\\S+) ([^\\s(]+)\\(([^)]*)\\)(?::\\d+:\\d+)? -> (\\S+)");
    private static final Pattern OBJECT_TYPE = Pattern.compile("L([^;]+);");
    private static final Map<String, String> PRIMITIVES = Map.of(
            "void", "V", "boolean", "Z", "byte", "B", "char", "C", "short", "S", "int", "I", "long", "J", "float",
            "F", "double", "D");
    private static final String ARRAY_SUFFIX = "[]";

    private final Map<String, String> officialByNamedClass = new HashMap<>();
    private final Map<String, String> namedByOfficialClass = new HashMap<>();
    private final Map<String, String> namedByOfficialMethod = new HashMap<>();

    private MojangNames() {}

    public static MojangNames read(Path mappings) throws IOException {
        MojangNames names = new MojangNames();
        names.readClasses(mappings);
        names.readMethods(mappings);
        return names;
    }

    public Optional<String> namedMethodOf(String officialOwner, String officialName, String officialDescriptor) {
        String namedOwner = namedByOfficialClass.get(officialOwner);
        String namedMethod = namedByOfficialMethod.get(methodKey(officialOwner, officialName, officialDescriptor));
        if (namedOwner == null || namedMethod == null) {
            return Optional.empty();
        }
        return Optional.of(namedOwner + "." + namedMethod + namedDescriptorOf(officialDescriptor));
    }

    private void readClasses(Path mappings) throws IOException {
        try (BufferedReader lines = Files.newBufferedReader(mappings, StandardCharsets.UTF_8)) {
            for (String line = lines.readLine(); line != null; line = lines.readLine()) {
                Matcher header = CLASS_LINE.matcher(line);
                if (header.matches()) {
                    String named = internalNameOf(header.group(1));
                    String official = internalNameOf(header.group(2));
                    officialByNamedClass.put(named, official);
                    namedByOfficialClass.put(official, named);
                }
            }
        }
    }

    private void readMethods(Path mappings) throws IOException {
        try (BufferedReader lines = Files.newBufferedReader(mappings, StandardCharsets.UTF_8)) {
            String officialOwner = null;
            for (String line = lines.readLine(); line != null; line = lines.readLine()) {
                Matcher header = CLASS_LINE.matcher(line);
                if (header.matches()) {
                    officialOwner = internalNameOf(header.group(2));
                    continue;
                }
                Matcher method = METHOD_LINE.matcher(line);
                if (officialOwner != null && method.matches()) {
                    String descriptor = officialDescriptorOf(method.group(3), method.group(1));
                    namedByOfficialMethod.put(methodKey(officialOwner, method.group(4), descriptor), method.group(2));
                }
            }
        }
    }

    private String officialDescriptorOf(String namedParameters, String namedReturnType) {
        StringBuilder descriptor = new StringBuilder("(");
        if (!namedParameters.isEmpty()) {
            for (String parameter : namedParameters.split(",")) {
                descriptor.append(officialTypeOf(parameter));
            }
        }
        return descriptor.append(')').append(officialTypeOf(namedReturnType)).toString();
    }

    private String officialTypeOf(String namedType) {
        StringBuilder descriptor = new StringBuilder();
        String element = namedType;
        while (element.endsWith(ARRAY_SUFFIX)) {
            descriptor.append('[');
            element = element.substring(0, element.length() - ARRAY_SUFFIX.length());
        }
        String primitive = PRIMITIVES.get(element);
        if (primitive != null) {
            return descriptor.append(primitive).toString();
        }
        String named = internalNameOf(element);
        return descriptor.append('L').append(officialByNamedClass.getOrDefault(named, named)).append(';').toString();
    }

    private String namedDescriptorOf(String officialDescriptor) {
        Matcher objectType = OBJECT_TYPE.matcher(officialDescriptor);
        StringBuilder named = new StringBuilder();
        while (objectType.find()) {
            String official = objectType.group(1);
            String replacement = "L" + namedByOfficialClass.getOrDefault(official, official) + ";";
            objectType.appendReplacement(named, Matcher.quoteReplacement(replacement));
        }
        objectType.appendTail(named);
        return named.toString();
    }

    private static String methodKey(String owner, String name, String descriptor) {
        return owner + "." + name + descriptor;
    }

    private static String internalNameOf(String binaryName) {
        return binaryName.replace('.', '/');
    }
}
