package io.github.luasgraphicss.minemod.compatibility.translation.texture;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import io.github.luasgraphicss.minemod.compatibility.contracts.support.OperationSupport;
import io.github.luasgraphicss.minemod.compatibility.resources.alias.VanillaAliases;
import io.github.luasgraphicss.minemod.compatibility.resources.redefine.TextureRedefinition;
import io.github.luasgraphicss.minemod.compatibility.translation.call.CallObserver;
import io.github.luasgraphicss.minemod.compatibility.translation.provider.ProviderCalls;
import io.github.luasgraphicss.minemod.compatibility.translation.report.OperationReporter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class AliasStorageGuard implements CallObserver {
    private static final String BIND_TEXTURE = "glBindTexture";
    private static final Set<String> STORAGE_DEFINITIONS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "glTexImage1D", "glTexImage2D", "glTexImage3D", "glTexImage2DMultisample", "glTexImage3DMultisample",
            "glTexStorage1D", "glTexStorage2D", "glTexStorage3D",
            "glCompressedTexImage1D", "glCompressedTexImage2D", "glCompressedTexImage3D",
            "glCopyTexImage1D", "glCopyTexImage2D")));

    private final TextureBindings bindings;
    private final VanillaAliases aliases;
    private final TextureRedefinition redefinition;
    private final ProviderCalls provider;
    private final OperationReporter reporter;

    public AliasStorageGuard(
            TextureBindings bindings,
            VanillaAliases aliases,
            TextureRedefinition redefinition,
            ProviderCalls provider,
            OperationReporter reporter) {
        this.bindings = Objects.requireNonNull(bindings, "bindings");
        this.aliases = Objects.requireNonNull(aliases, "aliases");
        this.redefinition = Objects.requireNonNull(redefinition, "redefinition");
        this.provider = Objects.requireNonNull(provider, "provider");
        this.reporter = Objects.requireNonNull(reporter, "reporter");
    }

    @Override
    public void observe(String function, long[] arguments) {
        if (!STORAGE_DEFINITIONS.contains(function)) {
            return;
        }
        int target = (int) arguments[0];
        int name = bindings.boundName(target);
        if (!aliases.isAlias(name)) {
            return;
        }
        int privateStorage = redefinition.redefine(name);
        provider.invoke(BIND_TEXTURE, new long[] {TextureBindings.bindingTargetOf(target), privateStorage});
        reporter.reportOnObject(
                function + " on a vanilla texture", OperationSupport.BRIDGE_REQUIRED, ObjectKind.TEXTURE, name);
    }
}
