package io.github.luasgraphicss.minemod.verification.scenario.bootstrap.probe;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

public final class ProbeJar {
    private static final String MANIFEST_VERSION = "1.0";
    private static final String PREMAIN_CLASS = "Premain-Class";
    private static final List<Class<?>> CLASSES = List.of(DefinedClassRecorder.class, ClassLoadingProbe.class);

    private ProbeJar() {}

    public static Path write(Path jar) throws IOException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, MANIFEST_VERSION);
        manifest.getMainAttributes().putValue(PREMAIN_CLASS, DefinedClassRecorder.class.getName());
        try (OutputStream file = Files.newOutputStream(jar);
                JarOutputStream archive = new JarOutputStream(file, manifest)) {
            for (Class<?> probeClass : CLASSES) {
                String entry = probeClass.getName().replace('.', '/') + DefinedClassRecorder.CLASS_SUFFIX;
                archive.putNextEntry(new ZipEntry(entry));
                try (InputStream classFile = probeClass.getClassLoader().getResourceAsStream(entry)) {
                    if (classFile == null) {
                        throw new IOException("the class path has no " + entry);
                    }
                    classFile.transferTo(archive);
                }
                archive.closeEntry();
            }
        }
        return jar;
    }
}
