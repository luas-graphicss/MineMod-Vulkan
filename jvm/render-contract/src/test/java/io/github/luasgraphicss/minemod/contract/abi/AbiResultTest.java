package io.github.luasgraphicss.minemod.contract.abi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AbiResultTest {
    @Test
    void everyResultRoundTripsThroughItsCode() {
        for (AbiResult result : AbiResult.values()) {
            assertEquals(result, AbiResult.fromCode(result.code()));
        }
    }

    @Test
    void keepsTheCodesTheNativeHeaderPublishes() {
        assertEquals(0, AbiResult.OK.code());
        assertEquals(2, AbiResult.INCOMPATIBLE_ABI.code());
        assertEquals(11, AbiResult.STALE_HANDLE.code());
        assertEquals(14, AbiResult.INTERNAL_ERROR.code());
    }

    @Test
    void rejectsCodesOutsideTheContract() {
        assertThrows(IllegalArgumentException.class, () -> AbiResult.fromCode(-1));
        assertThrows(IllegalArgumentException.class, () -> AbiResult.fromCode(AbiResult.values().length));
    }
}
