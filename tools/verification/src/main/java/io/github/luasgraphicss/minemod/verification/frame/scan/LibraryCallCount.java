package io.github.luasgraphicss.minemod.verification.frame.scan;

import io.github.luasgraphicss.minemod.verification.hooks.scan.ClientArchive;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Predicate;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public final class LibraryCallCount {
    private LibraryCallCount() {}

    public static Map<String, Integer> byCallee(ClientArchive client, Predicate<String> isCountedOwner)
            throws IOException {
        Map<String, Integer> calls = new TreeMap<>();
        for (String className : client.classNames()) {
            Optional<byte[]> classFile = client.classFile(className);
            if (classFile.isPresent()) {
                count(classFile.get(), isCountedOwner, calls);
            }
        }
        return calls;
    }

    private static void count(byte[] classFile, Predicate<String> isCountedOwner, Map<String, Integer> calls) {
        new ClassReader(classFile)
                .accept(
                        new ClassVisitor(Opcodes.ASM9) {
                            @Override
                            public MethodVisitor visitMethod(
                                    int access, String method, String descriptor, String signature, String[] thrown) {
                                return new MethodVisitor(Opcodes.ASM9) {
                                    @Override
                                    public void visitMethodInsn(
                                            int opcode, String owner, String name, String called, boolean isInterface) {
                                        if (opcode == Opcodes.INVOKESTATIC && isCountedOwner.test(owner)) {
                                            calls.merge(owner + "." + name + called, 1, Integer::sum);
                                        }
                                    }
                                };
                            }
                        },
                        ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
    }
}
