package io.github.luasgraphicss.minemod.verification.cli;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

record Invocation(List<String> command, Map<String, String> options) {
    private static final String OPTION_PREFIX = "--";

    static Invocation parse(String[] arguments) {
        List<String> command = new ArrayList<>();
        Map<String, String> options = new LinkedHashMap<>();
        int index = 0;
        while (index < arguments.length && !arguments[index].startsWith(OPTION_PREFIX)) {
            command.add(arguments[index++]);
        }
        while (index < arguments.length) {
            String name = arguments[index++];
            if (!name.startsWith(OPTION_PREFIX) || index == arguments.length) {
                throw new UsageException("expected an option followed by its value, found: " + name);
            }
            if (options.put(name.substring(OPTION_PREFIX.length()), arguments[index++]) != null) {
                throw new UsageException("option given twice: " + name);
            }
        }
        return new Invocation(List.copyOf(command), Map.copyOf(options));
    }

    String commandLine() {
        return String.join(" ", command);
    }

    Optional<String> option(String name) {
        return Optional.ofNullable(options.get(name));
    }

    String requiredOption(String name) {
        return option(name).orElseThrow(() -> new UsageException("missing required option: " + OPTION_PREFIX + name));
    }

    void allowOnly(Set<String> names) {
        for (String name : options.keySet()) {
            if (!names.contains(name)) {
                throw new UsageException("unknown option: " + OPTION_PREFIX + name);
            }
        }
    }
}
