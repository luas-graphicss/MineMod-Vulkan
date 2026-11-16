package io.github.luasgraphicss.minemod.codec;

import io.github.luasgraphicss.minemod.contract.abi.AbiResult;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

final class CommandStreamCorpus {
    static final class Entry {
        final String name;
        final AbiResult expected;
        final byte[] bytes;

        Entry(String name, AbiResult expected, byte[] bytes) {
            this.name = name;
            this.expected = expected;
            this.bytes = bytes;
        }

        ByteBuffer stream() {
            return ByteBuffer.wrap(bytes);
        }
    }

    private static final int HEXADECIMAL = 16;

    private CommandStreamCorpus() {}

    static List<Entry> load() throws IOException {
        Path directory = Paths.get(System.getProperty("minemod.commandStreamCorpus"));
        List<Entry> entries = new ArrayList<>();
        try (DirectoryStream<Path> files = Files.newDirectoryStream(directory, "*.stream")) {
            for (Path file : files) {
                entries.add(read(file));
            }
        }
        return entries;
    }

    static Entry named(String name) throws IOException {
        for (Entry entry : load()) {
            if (entry.name.equals(name)) {
                return entry;
            }
        }
        throw new IOException("the corpus has no stream named " + name);
    }

    private static Entry read(Path file) throws IOException {
        String[] tokens = new String(Files.readAllBytes(file), StandardCharsets.US_ASCII).trim().split("\\s+");
        if (tokens.length < 2 || !tokens[0].equals("expect")) {
            throw new IOException(file + " does not start with 'expect <RESULT>'");
        }
        byte[] bytes = new byte[tokens.length - 2];
        for (int index = 0; index < bytes.length; index++) {
            bytes[index] = (byte) Integer.parseInt(tokens[index + 2], HEXADECIMAL);
        }
        return new Entry(file.getFileName().toString(), AbiResult.valueOf(tokens[1]), bytes);
    }
}
