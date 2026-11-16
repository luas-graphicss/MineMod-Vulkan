package io.github.luasgraphicss.minemod.compatibility.translation.legacy;

import io.github.luasgraphicss.minemod.compatibility.translation.call.CallObserver;
import io.github.luasgraphicss.minemod.compatibility.translation.provider.ProviderCalls;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class OpenConstructs implements CallObserver {
    private static final String BEGIN = "glBegin";
    private static final String END = "glEnd";
    private static final String NEW_LIST = "glNewList";
    private static final String END_LIST = "glEndList";
    private static final String MATRIX_MODE = "glMatrixMode";
    private static final String PUSH_MATRIX = "glPushMatrix";
    private static final String POP_MATRIX = "glPopMatrix";
    private static final int GL_COMPILE_AND_EXECUTE = 0x1301;
    private static final int GL_MODELVIEW = 0x1700;
    private static final long[] NO_ARGUMENTS = new long[0];

    private final Map<Integer, Integer> matrixBalances = new TreeMap<>();
    private int matrixMode = GL_MODELVIEW;
    private boolean primitiveOpen;
    private boolean compilingList;
    private boolean executingWhileCompiling;

    @Override
    public void observe(String function, long[] arguments) {
        switch (function) {
            case BEGIN:
                primitiveOpen = true;
                break;
            case END:
                primitiveOpen = false;
                break;
            case NEW_LIST:
                compilingList = true;
                executingWhileCompiling = arguments[1] == GL_COMPILE_AND_EXECUTE;
                break;
            case END_LIST:
                compilingList = false;
                break;
            case MATRIX_MODE:
                matrixMode = (int) arguments[0];
                break;
            case PUSH_MATRIX:
                matrixBalances.merge(matrixMode, 1, Integer::sum);
                break;
            case POP_MATRIX:
                matrixBalances.merge(matrixMode, -1, Integer::sum);
                break;
            default:
                break;
        }
    }

    public boolean isPrimitiveOpen() {
        return primitiveOpen;
    }

    public boolean isCompilingList() {
        return compilingList;
    }

    public boolean defersExecution() {
        return compilingList && !executingWhileCompiling;
    }

    public void beginSection() {
        matrixBalances.clear();
    }

    public List<String> closeSection(ProviderCalls provider) {
        List<String> leftOpen = new ArrayList<>();
        if (primitiveOpen) {
            provider.invoke(END, NO_ARGUMENTS);
            primitiveOpen = false;
            leftOpen.add("a glBegin primitive was left open");
        }
        if (compilingList) {
            provider.invoke(END_LIST, NO_ARGUMENTS);
            compilingList = false;
            leftOpen.add("a display list was left open");
        }
        for (Map.Entry<Integer, Integer> balance : matrixBalances.entrySet()) {
            if (balance.getValue() != 0) {
                leftOpen.add("matrix stack 0x" + Integer.toHexString(balance.getKey()) + " was left "
                        + balance.getValue() + " level(s) from where it started");
            }
        }
        return leftOpen;
    }
}
