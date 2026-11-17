package io.github.luasgraphicss.minemod.compatibility.layer.route;

import io.github.luasgraphicss.minemod.compatibility.contracts.support.OperationSupport;
import io.github.luasgraphicss.minemod.compatibility.layer.memory.NativeMemory;
import io.github.luasgraphicss.minemod.compatibility.state.error.ErrorLedger;
import io.github.luasgraphicss.minemod.compatibility.translation.legacy.DisplayListCalls;
import io.github.luasgraphicss.minemod.compatibility.translation.legacy.DisplayListNames;
import io.github.luasgraphicss.minemod.compatibility.translation.report.OperationReporter;
import java.util.Optional;

final class ListRoutes {
    private static final int GL_BYTE = 0x1400;
    private static final int GL_UNSIGNED_BYTE = 0x1401;
    private static final int GL_SHORT = 0x1402;
    private static final int GL_UNSIGNED_SHORT = 0x1403;
    private static final int GL_INT = 0x1404;
    private static final int GL_UNSIGNED_INT = 0x1405;
    private static final int GL_INVALID_ENUM = 0x0500;
    private static final int GL_INVALID_VALUE = 0x0501;
    private static final long TRUE = 1;
    private static final long FALSE = 0;
    private final DisplayListNames names;
    private final DisplayListCalls calls;
    private final ErrorLedger errors;
    private final OperationReporter reporter;
    private final NativeMemory memory;

    ListRoutes(
            DisplayListNames names,
            DisplayListCalls calls,
            ErrorLedger errors,
            OperationReporter reporter,
            NativeMemory memory) {
        this.names = names;
        this.calls = calls;
        this.errors = errors;
        this.reporter = reporter;
        this.memory = memory;
    }

    Optional<Long> call(String coreFunction, long[] words) {
        switch (coreFunction) {
            case "glGenLists":
                return Optional.of((long) names.generate((int) words[0]));
            case "glDeleteLists":
                names.delete((int) words[0], (int) words[1]);
                return Optional.of(0L);
            case "glIsList":
                return Optional.of(names.isList((int) words[0]) ? TRUE : FALSE);
            case "glListBase":
                names.setBase((int) words[0]);
                return Optional.of(0L);
            case "glCallLists":
                callLists((int) words[0], (int) words[1], words[2]);
                return Optional.of(0L);
            default:
                return Optional.empty();
        }
    }

    private void callLists(int count, int type, long address) {
        if (count < 0) {
            errors.raise(GL_INVALID_VALUE);
            return;
        }
        int[] offsets = new int[count];
        for (int index = 0; index < count; index++) {
            Optional<Integer> offset = offsetAt(type, address, index);
            if (!offset.isPresent()) {
                reporter.report("glCallLists with offsets of type 0x" + Integer.toHexString(type), OperationSupport.UNVERIFIED);
                errors.raise(GL_INVALID_ENUM);
                return;
            }
            offsets[index] = offset.get();
        }
        calls.callLists(offsets);
    }

    private Optional<Integer> offsetAt(int type, long address, int index) {
        switch (type) {
            case GL_BYTE:
                return Optional.of((int) memory.readByte(address + index));
            case GL_UNSIGNED_BYTE:
                return Optional.of(memory.readByte(address + index) & 0xFF);
            case GL_SHORT:
                return Optional.of((int) (short) unsignedShort(address + (long) index * Short.BYTES));
            case GL_UNSIGNED_SHORT:
                return Optional.of(unsignedShort(address + (long) index * Short.BYTES));
            case GL_INT:
            case GL_UNSIGNED_INT:
                return Optional.of(memory.readInt(address + (long) index * Integer.BYTES));
            default:
                return Optional.empty();
        }
    }

    private int unsignedShort(long address) {
        return (memory.readByte(address) & 0xFF) | ((memory.readByte(address + 1) & 0xFF) << Byte.SIZE);
    }
}
