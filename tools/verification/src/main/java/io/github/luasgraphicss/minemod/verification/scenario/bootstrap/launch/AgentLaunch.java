package io.github.luasgraphicss.minemod.verification.scenario.bootstrap.launch;

import io.github.luasgraphicss.minemod.verification.scenario.bootstrap.probe.ClassLoadingProbe;
import io.github.luasgraphicss.minemod.verification.scenario.bootstrap.probe.DefinedClassRecorder;
import io.github.luasgraphicss.minemod.verification.scenario.bootstrap.probe.ProbeJar;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class AgentLaunch {
    public record Outcome(int exitCode, String output, Map<String, byte[]> definedClasses) {}

    private static final long TIMEOUT_SECONDS = 120;
    private static final int MAX_OUTPUT_BYTES = 64 * 1024;
    private static final String PROBE_JAR = "probe.jar";
    private static final String DEFINED_CLASSES = "defined";
    private static final String OUTPUT = "output.txt";

    private AgentLaunch() {}

    public static Outcome run(List<String> jvmArguments, List<Path> classPath, Set<String> classNames, Path scratch)
            throws IOException, InterruptedException {
        Files.createDirectories(scratch);
        Path probe = ProbeJar.write(scratch.resolve(PROBE_JAR));
        Path defined = scratch.resolve(DEFINED_CLASSES);
        Path output = scratch.resolve(OUTPUT);

        List<String> command = new ArrayList<>();
        command.add(javaExecutable());
        command.addAll(jvmArguments);
        command.add("-javaagent:" + probe + "=" + DefinedClassRecorder.options(defined, classNames));
        command.add("-cp");
        command.add(Stream.concat(classPath.stream(), Stream.of(probe))
                .map(Path::toString)
                .collect(Collectors.joining(File.pathSeparator)));
        command.add(ClassLoadingProbe.class.getName());
        command.addAll(classNames);

        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .redirectOutput(output.toFile())
                .start();
        if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IOException("the child JVM did not end within " + TIMEOUT_SECONDS + " seconds");
        }
        return new Outcome(process.exitValue(), startOf(output), classesIn(defined, classNames));
    }

    private static String javaExecutable() throws IOException {
        return ProcessHandle.current()
                .info()
                .command()
                .orElseThrow(() -> new IOException("the running JVM does not tell its executable"));
    }

    private static String startOf(Path output) throws IOException {
        try (InputStream content = Files.newInputStream(output)) {
            return new String(content.readNBytes(MAX_OUTPUT_BYTES), StandardCharsets.UTF_8);
        }
    }

    private static Map<String, byte[]> classesIn(Path directory, Set<String> classNames) throws IOException {
        Map<String, byte[]> classes = new LinkedHashMap<>();
        for (String className : classNames) {
            Path file = directory.resolve(className + DefinedClassRecorder.CLASS_SUFFIX);
            if (Files.isRegularFile(file)) {
                classes.put(className, Files.readAllBytes(file));
            }
        }
        return classes;
    }
}
