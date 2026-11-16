package io.github.luasgraphicss.minemod.compatibility.natives.gate;

import io.github.luasgraphicss.minemod.compatibility.dispatch.install.FunctionLookup;
import io.github.luasgraphicss.minemod.compatibility.dispatch.table.FunctionSource;
import io.github.luasgraphicss.minemod.compatibility.natives.binding.CallTable;
import io.github.luasgraphicss.minemod.compatibility.natives.binding.NativeBinding;
import java.util.OptionalInt;

public final class Thunks implements FunctionSource {
    @Override
    public long addressOf(String function) {
        OptionalInt index = CallTable.loaded().indexOf(function);
        return index.isPresent() ? NativeBinding.thunkAddress(index.getAsInt()) : FunctionLookup.UNAVAILABLE;
    }
}
