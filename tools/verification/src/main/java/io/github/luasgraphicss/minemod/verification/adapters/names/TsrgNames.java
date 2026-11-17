package io.github.luasgraphicss.minemod.verification.adapters.names;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class TsrgNames {
    private static final String SECOND_FORMAT_HEADER = "tsrg2 ";
    private static final String MEMBER_INDENT = "\t";
    private static final String NESTED_INDENT = "\t\t";
    private static final String WORD_SEPARATOR = " ";
    private static final String DESCRIPTOR_START = "(";
    private static final int CLASS_WORDS = 2;
    private static final int METHOD_WORDS = 3;

    public record SrgNames(NameTable table, boolean leavesClassNamesToMojang) {}

    private TsrgNames() {}

    public static SrgNames read(BufferedReader lines) throws IOException {
        Map<String, String> classes = new HashMap<>();
        Map<String, String> methods = new HashMap<>();
        boolean isSecondFormat = false;
        String owner = null;
        for (String line = lines.readLine(); line != null; line = lines.readLine()) {
            if (line.startsWith(SECOND_FORMAT_HEADER)) {
                isSecondFormat = true;
                continue;
            }
            if (line.isBlank() || line.startsWith(NESTED_INDENT)) {
                continue;
            }
            String[] words = line.trim().split(WORD_SEPARATOR);
            if (!line.startsWith(MEMBER_INDENT)) {
                if (words.length < CLASS_WORDS) {
                    throw new IOException("expected a class and its SRG name, found: " + line);
                }
                owner = words[0];
                classes.put(owner, words[1]);
            } else if (owner != null && words.length >= METHOD_WORDS && words[1].startsWith(DESCRIPTOR_START)) {
                methods.put(NameTable.methodKey(owner, words[0], words[1]), words[2]);
            }
        }
        if (owner == null) {
            throw new IOException("the TSRG file names no class");
        }
        return new SrgNames(new NameTable(isSecondFormat ? Map.of() : classes, methods), isSecondFormat);
    }
}
