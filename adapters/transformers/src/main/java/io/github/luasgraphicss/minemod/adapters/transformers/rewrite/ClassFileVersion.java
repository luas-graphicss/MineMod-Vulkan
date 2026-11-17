package io.github.luasgraphicss.minemod.adapters.transformers.rewrite;

import org.objectweb.asm.Opcodes;

final class ClassFileVersion {
    private static final int HEADER_BYTES = 8;
    private static final int MAJOR_VERSION_OFFSET = 6;
    private static final int JAVA_8 = 52;

    private ClassFileVersion() {}

    static int asmApiOf(byte[] classFile) {
        if (classFile.length < HEADER_BYTES) {
            throw new IllegalArgumentException("a class file has at least " + HEADER_BYTES + " bytes");
        }
        int major = (classFile[MAJOR_VERSION_OFFSET] & 0xFF) << 8 | (classFile[MAJOR_VERSION_OFFSET + 1] & 0xFF);
        return major <= JAVA_8 ? Opcodes.ASM5 : Opcodes.ASM9;
    }
}
