package io.github.luasgraphicss.minemod.adapters.loaders.launch.fixtures;

import io.github.luasgraphicss.minemod.adapters.loaders.launch.plan.PinnedPlan;
import io.github.luasgraphicss.minemod.adapters.transformers.point.MethodTarget;
import io.github.luasgraphicss.minemod.adapters.transformers.release.ReleasePlan;
import io.github.luasgraphicss.minemod.adapters.transformers.release.ReleasePlanFile;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public final class FixtureGame {
    public static final String RELEASE = "fixture";
    public static final String WINDOW = "fixture/Window";
    public static final String UNPINNED_CLIENT = "0123456789abcdef0123456789abcdef01234567";
    public static final MethodTarget SWAP_BUFFERS = MethodTarget.parse("org/lwjgl/glfw/GLFW.glfwSwapBuffers(J)V");
    public static final MethodTarget DISPLAY_UPDATE = MethodTarget.parse("org/lwjgl/opengl/Display.update()V");

    private static final String PRESENT = "present";
    private static final String PRESENT_DESCRIPTOR = "()V";
    private static final String CLASS_SUFFIX = ".class";

    private FixtureGame() {}

    public static byte[] windowCalling(MethodTarget library, int calls) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, WINDOW, null, "java/lang/Object", null);
        MethodVisitor present = writer.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, PRESENT, PRESENT_DESCRIPTOR, null, null);
        present.visitCode();
        for (int call = 0; call < calls; call++) {
            for (Type argument : Type.getArgumentTypes(library.descriptor())) {
                present.visitInsn(argument.getSize() == 2 ? Opcodes.LCONST_0 : Opcodes.ICONST_0);
            }
            present.visitMethodInsn(
                    Opcodes.INVOKESTATIC, library.owner(), library.name(), library.descriptor(), false);
        }
        present.visitInsn(Opcodes.RETURN);
        present.visitMaxs(0, 0);
        present.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    public static String planText(MethodTarget library, String clientSha1) {
        return "release=" + RELEASE + "\n"
                + "client.sha1=" + clientSha1 + "\n"
                + "points=present\n"
                + "point.present.callee=" + library + "\n"
                + "point.present.within=" + WINDOW + "." + PRESENT + PRESENT_DESCRIPTOR + "\n"
                + "point.present.matches=1\n";
    }

    public static ReleasePlan plan(MethodTarget library) {
        byte[] text = planText(library, UNPINNED_CLIENT).getBytes(StandardCharsets.UTF_8);
        try {
            return ReleasePlanFile.read(new ByteArrayInputStream(text));
        } catch (IOException unreadable) {
            throw new UncheckedIOException(unreadable);
        }
    }

    public static Path writePlan(Path adapterRoot, MethodTarget library, String clientSha1) throws IOException {
        Path plan = adapterRoot.resolve(PinnedPlan.RESOURCE);
        Files.createDirectories(plan.getParent());
        return Files.write(plan, planText(library, clientSha1).getBytes(StandardCharsets.UTF_8));
    }

    public static Path writeWindow(Path classesRoot, byte[] classFile) throws IOException {
        Path file = classesRoot.resolve(WINDOW + CLASS_SUFFIX);
        Files.createDirectories(file.getParent());
        return Files.write(file, classFile);
    }

    public static int callsTo(byte[] classFile, String owner) {
        int[] calls = {0};
        new ClassReader(classFile)
                .accept(
                        new ClassVisitor(Opcodes.ASM5) {
                            @Override
                            public MethodVisitor visitMethod(
                                    int access, String name, String descriptor, String signature, String[] exceptions) {
                                return new MethodVisitor(Opcodes.ASM5) {
                                    @Override
                                    public void visitMethodInsn(
                                            int opcode,
                                            String calledOwner,
                                            String calledName,
                                            String calledDescriptor,
                                            boolean isInterface) {
                                        if (calledOwner.equals(owner)) {
                                            calls[0]++;
                                        }
                                    }
                                };
                            }
                        },
                        0);
        return calls[0];
    }
}
