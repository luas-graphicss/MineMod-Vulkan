package io.github.luasgraphicss.minemod.compatibility.dispatch.table;

import io.github.luasgraphicss.minemod.compatibility.contracts.support.OperationSupport;
import io.github.luasgraphicss.minemod.compatibility.dispatch.catalog.CallCatalog;
import io.github.luasgraphicss.minemod.compatibility.dispatch.catalog.CallDescription;
import io.github.luasgraphicss.minemod.compatibility.dispatch.catalog.Interception;
import io.github.luasgraphicss.minemod.compatibility.dispatch.install.FunctionLookup;
import io.github.luasgraphicss.minemod.compatibility.provider.capability.ContextProfile;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class DispatchTable implements FunctionLookup {
    private final CallCatalog catalog;
    private final ContextProfile profile;
    private final FunctionSource provider;
    private final LibraryImage providerImage;
    private final FunctionSource thunks;
    private final Map<String, DispatchEntry> entries = new LinkedHashMap<>();

    public DispatchTable(
            CallCatalog catalog,
            ContextProfile profile,
            FunctionSource provider,
            LibraryImage providerImage,
            FunctionSource thunks) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.profile = Objects.requireNonNull(profile, "profile");
        this.provider = Objects.requireNonNull(provider, "provider");
        this.providerImage = Objects.requireNonNull(providerImage, "providerImage");
        this.thunks = Objects.requireNonNull(thunks, "thunks");
    }

    @Override
    public long addressOf(String function) {
        return entry(function).address();
    }

    public synchronized DispatchEntry entry(String function) {
        DispatchEntry known = entries.get(function);
        if (known != null) {
            return known;
        }
        DispatchEntry resolved = resolve(function);
        entries.put(function, resolved);
        return resolved;
    }

    public synchronized List<DispatchEntry> entries() {
        return new ArrayList<>(entries.values());
    }

    private DispatchEntry resolve(String function) {
        Optional<CallDescription> description = catalog.describe(function);
        if (!description.isPresent()) {
            return withheld(function, OperationSupport.UNVERIFIED);
        }
        if (description.get().family().isLegacy() && profile == ContextProfile.CORE) {
            return withheld(function, OperationSupport.OUT_OF_CONTRACT);
        }
        long providerAddress = provider.addressOf(function);
        if (providerAddress == UNAVAILABLE) {
            return withheld(function, OperationSupport.OUT_OF_CONTRACT);
        }
        if (!providerImage.contains(providerAddress)) {
            throw new ForeignFunctionException(function);
        }
        if (description.get().interception() == Interception.FORWARD) {
            return new DispatchEntry(function, Disposition.PROVIDER, providerAddress, OperationSupport.SUPPORTED);
        }
        long thunkAddress = thunks.addressOf(function);
        if (thunkAddress == UNAVAILABLE) {
            return withheld(function, OperationSupport.UNVERIFIED);
        }
        return new DispatchEntry(function, Disposition.THUNK, thunkAddress, OperationSupport.SUPPORTED);
    }

    private static DispatchEntry withheld(String function, OperationSupport support) {
        return new DispatchEntry(function, Disposition.WITHHELD, UNAVAILABLE, support);
    }
}
