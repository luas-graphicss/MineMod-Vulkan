package io.github.luasgraphicss.minemod.verification.frame.catalog;

import io.github.luasgraphicss.minemod.verification.hooks.catalog.LibraryCall;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public final class StandIns {
    private static final String SHARED_INDEX = "META-INF/minemod/frame/stand-ins.properties";
    private static final String GENERATION_INDEX = "META-INF/minemod/frame/stand-ins-lwjgl%d.properties";
    private static final String CLASS_SUFFIX = ".class";
    private static final String OWNER_SEPARATOR = ",";
    private static final int ANSWERING = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC;

    private final Map<String, String> standInsByCallee;
    private final List<LibraryCall> calls;

    private StandIns(Map<String, String> standInsByCallee, List<LibraryCall> calls) {
        this.standInsByCallee = Map.copyOf(standInsByCallee);
        this.calls = List.copyOf(calls);
    }

    public static StandIns ofLwjglGeneration(int generation) throws IOException {
        ClassLoader classes = StandIns.class.getClassLoader();
        Map<String, String> standInsByCallee = new LinkedHashMap<>();
        List<LibraryCall> calls = new ArrayList<>();
        for (String index : List.of(SHARED_INDEX, GENERATION_INDEX.formatted(generation))) {
            for (URL listed : Collections.list(classes.getResources(index))) {
                Properties ownersByStandIn = new Properties();
                try (InputStream content = listed.openStream()) {
                    ownersByStandIn.load(content);
                }
                for (String standIn : new TreeSet<>(ownersByStandIn.stringPropertyNames())) {
                    List<String> owners = List.of(ownersByStandIn.getProperty(standIn).trim().split(OWNER_SEPARATOR));
                    register(classes, standIn, owners, standInsByCallee, calls);
                }
            }
        }
        if (calls.isEmpty()) {
            throw new IOException("no class on the class path stands in for LWJGL " + generation);
        }
        return new StandIns(standInsByCallee, calls);
    }

    public List<LibraryCall> calls() {
        return calls;
    }

    public Set<String> owners() {
        Set<String> owners = new TreeSet<>();
        calls.forEach(call -> owners.add(call.owner()));
        return owners;
    }

    public Optional<String> answering(String calleeSignature) {
        return Optional.ofNullable(standInsByCallee.get(calleeSignature));
    }

    private static void register(
            ClassLoader classes,
            String standIn,
            List<String> owners,
            Map<String, String> standInsByCallee,
            List<LibraryCall> calls)
            throws IOException {
        try (InputStream classFile = classes.getResourceAsStream(standIn + CLASS_SUFFIX)) {
            if (classFile == null) {
                throw new IOException("the listed stand-in " + standIn + " is not on the class path");
            }
            new ClassReader(classFile)
                    .accept(
                            new ClassVisitor(Opcodes.ASM9) {
                                @Override
                                public MethodVisitor visitMethod(
                                        int access, String name, String descriptor, String signature, String[] thrown) {
                                    if ((access & ANSWERING) == ANSWERING) {
                                        for (String owner : owners) {
                                            answer(standIn, owner.trim(), name, descriptor, standInsByCallee, calls);
                                        }
                                    }
                                    return null;
                                }
                            },
                            ClassReader.SKIP_CODE);
        }
    }

    private static void answer(
            String standIn,
            String owner,
            String name,
            String descriptor,
            Map<String, String> standInsByCallee,
            List<LibraryCall> calls) {
        String pointName = owner.substring(owner.lastIndexOf('/') + 1) + "." + name + descriptor;
        LibraryCall call = new LibraryCall(pointName, owner, name, descriptor);
        String earlier = standInsByCallee.put(call.signature(), standIn);
        if (earlier != null) {
            throw new IllegalStateException(call.signature() + " is answered by both " + earlier + " and " + standIn);
        }
        calls.add(call);
    }
}
