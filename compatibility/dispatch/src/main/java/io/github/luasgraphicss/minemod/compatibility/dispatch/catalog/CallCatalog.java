package io.github.luasgraphicss.minemod.compatibility.dispatch.catalog;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class CallCatalog {
    private static final String RESOURCE = "gl-calls.txt";
    private static final List<String> ALIASED_SUFFIXES = Arrays.asList("ARB", "EXT");

    private final Map<String, CallDescription> descriptions = new LinkedHashMap<>();
    private final ExtensionCatalog extensions;

    public CallCatalog(Collection<CallDescription> described, ExtensionCatalog extensions) {
        for (CallDescription description : described) {
            if (descriptions.put(description.function(), description) != null) {
                throw new IllegalArgumentException(description.function() + " is described twice");
            }
        }
        this.extensions = Objects.requireNonNull(extensions, "extensions");
    }

    public static CallCatalog standard() {
        return Standard.CATALOG;
    }

    public Optional<CallDescription> describe(String function) {
        CallDescription exact = descriptions.get(function);
        if (exact != null) {
            return Optional.of(exact);
        }
        if (!extensions.isMember(function)) {
            return Optional.empty();
        }
        for (String suffix : ALIASED_SUFFIXES) {
            if (function.endsWith(suffix)) {
                String coreFunction = function.substring(0, function.length() - suffix.length());
                return Optional.ofNullable(descriptions.get(coreFunction));
            }
        }
        return Optional.empty();
    }

    public Collection<CallDescription> descriptions() {
        return Collections.unmodifiableCollection(descriptions.values());
    }

    public ExtensionCatalog extensions() {
        return extensions;
    }

    private static final class Standard {
        private static final CallCatalog CATALOG = new CallCatalog(
                CallCatalogParser.parse(CatalogResource.lines(RESOURCE)), ExtensionCatalog.standard());
    }
}
