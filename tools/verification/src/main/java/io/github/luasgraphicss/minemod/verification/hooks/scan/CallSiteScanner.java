package io.github.luasgraphicss.minemod.verification.hooks.scan;

import io.github.luasgraphicss.minemod.verification.hooks.catalog.LibraryCall;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public final class CallSiteScanner {
    private final List<LibraryCall> calls;
    private final List<byte[]> ownerNames;

    public CallSiteScanner(List<LibraryCall> calls) {
        this.calls = List.copyOf(calls);
        this.ownerNames = calls.stream()
                .map(LibraryCall::owner)
                .distinct()
                .map(owner -> owner.getBytes(StandardCharsets.UTF_8))
                .toList();
    }

    public List<CallSite> scan(ClientArchive archive) throws IOException {
        List<CallSite> sites = new ArrayList<>();
        for (String className : archive.classNames()) {
            Optional<byte[]> classFile = archive.classFile(className);
            if (classFile.isPresent() && namesAnOwner(classFile.get())) {
                sites.addAll(scan(classFile.get()));
            }
        }
        return sites;
    }

    public List<CallSite> scan(byte[] classFile) {
        List<CallSite> sites = new ArrayList<>();
        ClassReader reader = new ClassReader(classFile);
        String owner = reader.getClassName();
        reader.accept(
                new ClassVisitor(Opcodes.ASM9) {
                    @Override
                    public MethodVisitor visitMethod(
                            int access, String method, String descriptor, String signature, String[] exceptions) {
                        return new MethodVisitor(Opcodes.ASM9) {
                            @Override
                            public void visitMethodInsn(
                                    int opcode,
                                    String calledOwner,
                                    String calledName,
                                    String calledDescriptor,
                                    boolean isInterface) {
                                if (opcode != Opcodes.INVOKESTATIC) {
                                    return;
                                }
                                for (LibraryCall call : calls) {
                                    if (call.isCalledBy(calledOwner, calledName, calledDescriptor)) {
                                        sites.add(new CallSite(call, owner, method, descriptor));
                                    }
                                }
                            }
                        };
                    }
                },
                ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return sites;
    }

    private boolean namesAnOwner(byte[] classFile) {
        for (byte[] ownerName : ownerNames) {
            if (indexOf(classFile, ownerName) >= 0) {
                return true;
            }
        }
        return false;
    }

    private static int indexOf(byte[] content, byte[] sought) {
        for (int start = 0; start <= content.length - sought.length; start++) {
            int matched = 0;
            while (matched < sought.length && content[start + matched] == sought[matched]) {
                matched++;
            }
            if (matched == sought.length) {
                return start;
            }
        }
        return -1;
    }
}
