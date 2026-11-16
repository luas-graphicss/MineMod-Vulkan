package io.github.luasgraphicss.minemod.compatibility.dispatch.exposure;

import io.github.luasgraphicss.minemod.compatibility.dispatch.catalog.ExtensionCatalog;
import io.github.luasgraphicss.minemod.compatibility.dispatch.table.DispatchTable;
import io.github.luasgraphicss.minemod.compatibility.dispatch.table.Disposition;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

public final class ExtensionExposure {
    private ExtensionExposure() {
    }

    public static Set<String> exposed(
            Collection<String> providerExtensions, ExtensionCatalog approved, DispatchTable table) {
        Set<String> exposed = new TreeSet<>();
        for (String extension : providerExtensions) {
            if (approved.isApproved(extension) && everyFunctionResolves(approved.functionsOf(extension), table)) {
                exposed.add(extension);
            }
        }
        return exposed;
    }

    private static boolean everyFunctionResolves(Set<String> functions, DispatchTable table) {
        for (String function : functions) {
            if (table.entry(function).disposition() == Disposition.WITHHELD) {
                return false;
            }
        }
        return true;
    }
}
