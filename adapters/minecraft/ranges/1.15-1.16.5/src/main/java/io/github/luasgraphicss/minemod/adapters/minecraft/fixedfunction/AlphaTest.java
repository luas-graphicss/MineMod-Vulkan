package io.github.luasgraphicss.minemod.adapters.minecraft.fixedfunction;

public final class AlphaTest {
    private static final int GREATER = 0x0204;
    private static final int GREATER_OR_EQUAL = 0x0206;
    private static final int ALWAYS = 0x0207;
    private static final float KEEP_ALL = 0.0f;

    private boolean isEnabled;
    private int function = ALWAYS;
    private float reference;

    public void enable(boolean isTested) {
        isEnabled = isTested;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void compare(int comparison, float against) {
        function = comparison;
        reference = against;
    }

    public float cutoff() {
        boolean cutsFaintTexels = function == GREATER || function == GREATER_OR_EQUAL;
        return isEnabled && cutsFaintTexels ? reference : KEEP_ALL;
    }
}
