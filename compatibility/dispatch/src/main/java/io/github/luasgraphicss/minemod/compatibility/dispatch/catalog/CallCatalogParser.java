package io.github.luasgraphicss.minemod.compatibility.dispatch.catalog;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import java.util.ArrayList;
import java.util.List;

public final class CallCatalogParser {
    private static final int FIXED_FIELDS = 4;
    private static final char NAME_SEPARATOR = ':';

    private CallCatalogParser() {
    }

    public static List<CallDescription> parse(List<String> lines) {
        List<CallDescription> descriptions = new ArrayList<>();
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index).trim();
            if (line.isEmpty()) {
                continue;
            }
            try {
                descriptions.addAll(parseLine(line));
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("line " + (index + 1) + ": " + exception.getMessage(), exception);
            }
        }
        return descriptions;
    }

    private static List<CallDescription> parseLine(String line) {
        String[] fields = line.split("\\s+");
        if (fields.length < FIXED_FIELDS) {
            throw new IllegalArgumentException("expected a function, its family, interception and checkpoint");
        }
        CallFamily family = CallFamily.valueOf(fields[1]);
        Interception interception = Interception.valueOf(fields[2]);
        Checkpoint checkpoint = Checkpoint.valueOf(fields[3]);
        List<NameArgument> nameArguments = new ArrayList<>();
        for (int field = FIXED_FIELDS; field < fields.length; field++) {
            nameArguments.add(parseNameArgument(fields[field]));
        }
        List<CallDescription> descriptions = new ArrayList<>();
        for (String function : BraceExpansion.expand(fields[0])) {
            descriptions.add(new CallDescription(function, family, interception, checkpoint, nameArguments));
        }
        return descriptions;
    }

    private static NameArgument parseNameArgument(String field) {
        int separator = field.indexOf(NAME_SEPARATOR);
        if (separator < 0) {
            throw new IllegalArgumentException("a name argument is written index:KIND, found " + field);
        }
        int index = Integer.parseInt(field.substring(0, separator));
        return new NameArgument(index, ObjectKind.valueOf(field.substring(separator + 1)));
    }
}
