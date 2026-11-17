package io.github.luasgraphicss.minemod.verification.hooks.fixtures;

import io.github.luasgraphicss.minemod.verification.hooks.catalog.LibraryCall;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public final class FixtureClient {
    private static final String CLASS_SUFFIX = ".class";

    private FixtureClient() {}

    public static byte[] classCalling(String className, Map<String, List<LibraryCall>> callsByMethod) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, className, null, "java/lang/Object", null);
        callsByMethod.forEach((method, calls) -> writeMethod(writer, method, calls));
        writer.visitEnd();
        return writer.toByteArray();
    }

    public static Path jarOf(Path jar, Map<String, byte[]> classFilesByName) throws IOException {
        try (OutputStream file = Files.newOutputStream(jar);
                ZipOutputStream archive = new ZipOutputStream(file)) {
            for (Map.Entry<String, byte[]> classFile : classFilesByName.entrySet()) {
                archive.putNextEntry(new ZipEntry(classFile.getKey() + CLASS_SUFFIX));
                archive.write(classFile.getValue());
                archive.closeEntry();
            }
        }
        return jar;
    }

    private static void writeMethod(ClassWriter writer, String method, List<LibraryCall> calls) {
        MethodVisitor body = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, method, "()V", null, null);
        body.visitCode();
        for (LibraryCall call : calls) {
            for (Type argument : Type.getArgumentTypes(call.descriptor())) {
                body.visitInsn(zeroOf(argument));
            }
            body.visitMethodInsn(Opcodes.INVOKESTATIC, call.owner(), call.name(), call.descriptor(), false);
            discard(body, Type.getReturnType(call.descriptor()));
        }
        body.visitInsn(Opcodes.RETURN);
        body.visitMaxs(0, 0);
        body.visitEnd();
    }

    private static int zeroOf(Type argument) {
        return switch (argument.getSort()) {
            case Type.LONG -> Opcodes.LCONST_0;
            case Type.OBJECT, Type.ARRAY -> Opcodes.ACONST_NULL;
            default -> Opcodes.ICONST_0;
        };
    }

    private static void discard(MethodVisitor body, Type returned) {
        if (returned.getSize() == 1) {
            body.visitInsn(Opcodes.POP);
        } else if (returned.getSize() == 2) {
            body.visitInsn(Opcodes.POP2);
        }
    }
}
