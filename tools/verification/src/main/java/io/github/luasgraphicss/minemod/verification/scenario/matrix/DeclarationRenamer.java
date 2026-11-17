package io.github.luasgraphicss.minemod.verification.scenario.matrix;

import io.github.luasgraphicss.minemod.adapters.transformers.point.MethodTarget;
import java.util.Map;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

final class DeclarationRenamer {
    private DeclarationRenamer() {}

    static byte[] rename(byte[] classFile, String runtimeClass, Map<String, MethodTarget> runtimeMethods) {
        ClassWriter renamed = new ClassWriter(0);
        new ClassReader(classFile)
                .accept(
                        new ClassVisitor(Opcodes.ASM9, renamed) {
                            @Override
                            public void visit(
                                    int version,
                                    int access,
                                    String name,
                                    String signature,
                                    String superName,
                                    String[] interfaces) {
                                super.visit(version, access, runtimeClass, signature, superName, interfaces);
                            }

                            @Override
                            public MethodVisitor visitMethod(
                                    int access, String name, String descriptor, String signature, String[] exceptions) {
                                MethodTarget runtime = runtimeMethods.get(name + descriptor);
                                return runtime == null
                                        ? super.visitMethod(access, name, descriptor, signature, exceptions)
                                        : super.visitMethod(
                                                access, runtime.name(), runtime.descriptor(), signature, exceptions);
                            }
                        },
                        0);
        return renamed.toByteArray();
    }
}
