package io.github.luasgraphicss.minemod.verification.adapters.names;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TinyNames {
    private static final String SEPARATOR = "\t";
    private static final List<String> HEADER = List.of("tiny", "2", "0", "official", "intermediary");
    private static final String CLASS = "c";
    private static final String METHOD = "m";
    private static final int CLASS_COLUMNS = 3;
    private static final int METHOD_COLUMNS = 5;

    private TinyNames() {}

    public static NameTable read(BufferedReader lines) throws IOException {
        String header = lines.readLine();
        if (header == null || !Arrays.asList(header.split(SEPARATOR)).equals(HEADER)) {
            throw new IOException("expected a Tiny v2 file from official to intermediary names, found: " + header);
        }
        Map<String, String> classes = new HashMap<>();
        Map<String, String> methods = new HashMap<>();
        String owner = null;
        for (String line = lines.readLine(); line != null; line = lines.readLine()) {
            String[] columns = line.split(SEPARATOR);
            if (columns.length == CLASS_COLUMNS && columns[0].equals(CLASS)) {
                owner = columns[1];
                classes.put(owner, columns[2]);
            } else if (owner != null
                    && columns.length == METHOD_COLUMNS
                    && columns[0].isEmpty()
                    && columns[1].equals(METHOD)) {
                methods.put(NameTable.methodKey(owner, columns[3], columns[2]), columns[4]);
            }
        }
        return new NameTable(classes, methods);
    }
}
