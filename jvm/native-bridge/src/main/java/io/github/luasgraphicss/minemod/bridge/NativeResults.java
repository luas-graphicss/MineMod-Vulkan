package io.github.luasgraphicss.minemod.bridge;

import io.github.luasgraphicss.minemod.contract.abi.AbiException;
import io.github.luasgraphicss.minemod.contract.abi.AbiResult;

final class NativeResults {
    private NativeResults() {}

    static void requireOk(int resultCode, String operation) {
        AbiResult result = AbiResult.fromCode(resultCode);
        if (result != AbiResult.OK) {
            throw new AbiException(result, operation);
        }
    }
}
