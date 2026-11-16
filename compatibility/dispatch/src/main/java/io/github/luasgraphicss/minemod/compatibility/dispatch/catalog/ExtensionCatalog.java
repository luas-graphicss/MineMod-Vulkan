package io.github.luasgraphicss.minemod.compatibility.dispatch.catalog;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ExtensionCatalog {
    private static final String RESOURCE = "gl-extensions.txt";

    private final Map<String, Set<String>> functionsByExtension = new LinkedHashMap<>();
    private final Set<String> members = new HashSet<>();

    public ExtensionCatalog(List<String> lines) {
        for (String line : lines) {
            String[] fields = line.trim().split("\\s+");
            if (fields[0].isEmpty()) {
                continue;
            }
            Set<String> functions = new LinkedHashSet<>();
            for (int field = 1; field < fields.length; field++) {
                functions.addAll(BraceExpansion.expand(fields[field]));
            }
            if (functionsByExtension.put(fields[0], Collections.unmodifiableSet(functions)) != null) {
                throw new IllegalArgumentException(fields[0] + " is listed twice");
            }
            members.addAll(functions);
        }
    }

    public static ExtensionCatalog standard() {
        return Standard.CATALOG;
    }

    public Set<String> extensions() {
        return Collections.unmodifiableSet(functionsByExtension.keySet());
    }

    public boolean isApproved(String extension) {
        return functionsByExtension.containsKey(extension);
    }

    public Set<String> functionsOf(String extension) {
        Set<String> functions = functionsByExtension.get(extension);
        if (functions == null) {
            throw new IllegalArgumentException(extension + " is not an approved extension");
        }
        return functions;
    }

    public boolean isMember(String function) {
        return members.contains(function);
    }

    private static final class Standard {
        private static final ExtensionCatalog CATALOG = new ExtensionCatalog(CatalogResource.lines(RESOURCE));
    }
}
