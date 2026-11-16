package io.github.luasgraphicss.minemod.compatibility.translation.texture;

import io.github.luasgraphicss.minemod.compatibility.translation.call.CallObserver;
import java.util.HashMap;
import java.util.Map;

public final class TextureBindings implements CallObserver {
    private static final String ACTIVE_TEXTURE = "glActiveTexture";
    private static final String BIND_TEXTURE = "glBindTexture";
    private static final int NOT_BOUND = 0;
    private static final int GL_TEXTURE0 = 0x84C0;
    private static final int GL_TEXTURE_CUBE_MAP = 0x8513;
    private static final int GL_CUBE_MAP_FIRST_FACE = 0x8515;
    private static final int GL_CUBE_MAP_LAST_FACE = 0x851A;

    private final Map<Long, Integer> namesByUnitAndTarget = new HashMap<>();
    private int activeUnit;

    @Override
    public void observe(String function, long[] arguments) {
        if (ACTIVE_TEXTURE.equals(function)) {
            activeUnit = (int) arguments[0] - GL_TEXTURE0;
        } else if (BIND_TEXTURE.equals(function)) {
            namesByUnitAndTarget.put(key(activeUnit, (int) arguments[0]), (int) arguments[1]);
        }
    }

    public int boundName(int target) {
        Integer name = namesByUnitAndTarget.get(key(activeUnit, bindingTargetOf(target)));
        return name == null ? NOT_BOUND : name;
    }

    public int activeUnit() {
        return activeUnit;
    }

    public void forEachBindingOf(int name, BindingVisitor visitor) {
        for (Map.Entry<Long, Integer> binding : namesByUnitAndTarget.entrySet()) {
            if (binding.getValue() == name) {
                visitor.visit((int) (binding.getKey() >>> Integer.SIZE), binding.getKey().intValue());
            }
        }
    }

    public static int bindingTargetOf(int target) {
        return target >= GL_CUBE_MAP_FIRST_FACE && target <= GL_CUBE_MAP_LAST_FACE ? GL_TEXTURE_CUBE_MAP : target;
    }

    private static long key(int unit, int target) {
        return ((long) unit << Integer.SIZE) | (target & 0xFFFFFFFFL);
    }

    public interface BindingVisitor {
        void visit(int unit, int target);
    }
}
