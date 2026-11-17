package io.github.luasgraphicss.minemod.verification.scenario.bootstrap.probe;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.ProtectionDomain;
import java.util.Set;

public final class DefinedClassRecorder implements ClassFileTransformer {
    public static final String CLASS_SUFFIX = ".class";
    private static final String NAME_SEPARATOR = ",";

    private final Path directory;
    private final Set<String> recordedClasses;

    private DefinedClassRecorder(Path directory, Set<String> recordedClasses) {
        this.directory = directory;
        this.recordedClasses = recordedClasses;
    }

    public static String options(Path directory, Set<String> recordedClasses) {
        return directory + File.pathSeparator + String.join(NAME_SEPARATOR, recordedClasses);
    }

    public static void premain(String options, Instrumentation instrumentation) {
        int separator = options.lastIndexOf(File.pathSeparatorChar);
        instrumentation.addTransformer(new DefinedClassRecorder(
                Path.of(options.substring(0, separator)),
                Set.of(options.substring(separator + 1).split(NAME_SEPARATOR))));
    }

    @Override
    public byte[] transform(
            ClassLoader loader,
            String className,
            Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain,
            byte[] classFile) {
        if (className != null && recordedClasses.contains(className)) {
            record(className, classFile);
        }
        return null;
    }

    private void record(String className, byte[] classFile) {
        Path file = directory.resolve(className + CLASS_SUFFIX);
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, classFile);
        } catch (IOException unwritable) {
            System.err.println("cannot record " + className + ": " + unwritable);
        }
    }
}
