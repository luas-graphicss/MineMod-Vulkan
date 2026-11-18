package io.github.luasgraphicss.minemod.verification.machines.probe;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class HarnessProcess {
    private static final long TIMEOUT_SECONDS = 120;
    private static final int MAX_OUTPUT_BYTES = 64 * 1024;
    private static final String VALIDATION = "--validation";
    private static final String FRAMES = "--frames";
    private static final String FRAME_COUNT = "3";

    private HarnessProcess() {}

    public static String run(Path executable) throws IOException, InterruptedException {
        if (!Files.isRegularFile(executable)) {
            throw new IOException(executable + " is not a file; build the harness and pass its executable");
        }
        Path captured = Files.createTempFile("minemod-harness", ".txt");
        try {
            Process process = new ProcessBuilder(List.of(executable.toString(), VALIDATION, FRAMES, FRAME_COUNT))
                    .redirectErrorStream(true)
                    .redirectOutput(captured.toFile())
                    .start();
            if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IOException("the harness did not end within " + TIMEOUT_SECONDS + " seconds");
            }
            String output = startOf(captured);
            if (process.exitValue() != 0) {
                throw new IOException("the harness exited with " + process.exitValue() + ": " + output.strip());
            }
            return output;
        } finally {
            Files.deleteIfExists(captured);
        }
    }

    private static String startOf(Path captured) throws IOException {
        try (InputStream content = Files.newInputStream(captured)) {
            return new String(content.readNBytes(MAX_OUTPUT_BYTES), StandardCharsets.UTF_8);
        }
    }
}
