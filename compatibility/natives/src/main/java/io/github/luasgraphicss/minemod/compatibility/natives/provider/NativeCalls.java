package io.github.luasgraphicss.minemod.compatibility.natives.provider;

import io.github.luasgraphicss.minemod.compatibility.natives.binding.CallTable;
import io.github.luasgraphicss.minemod.compatibility.natives.binding.NativeBinding;
import io.github.luasgraphicss.minemod.compatibility.natives.binding.NativeProviderException;
import io.github.luasgraphicss.minemod.compatibility.provider.load.LoadedProvider;
import io.github.luasgraphicss.minemod.compatibility.translation.provider.ProviderCalls;
import java.util.OptionalInt;

final class NativeCalls implements ProviderCalls {
    private final LoadedProvider loaded;

    NativeCalls(LoadedProvider loaded) {
        this.loaded = loaded;
    }

    @Override
    public long invoke(String function, long[] arguments) {
        OptionalInt index = CallTable.loaded().indexOf(function);
        if (!index.isPresent()) {
            throw new NativeProviderException(function + " is not in the native call table");
        }
        return NativeBinding.invoke(loaded.handle(), index.getAsInt(), arguments);
    }
}
