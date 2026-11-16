package io.github.luasgraphicss.minemod.verification.capability;

import io.github.luasgraphicss.minemod.compatibility.contracts.support.OperationSupport;
import io.github.luasgraphicss.minemod.compatibility.dispatch.catalog.CallCatalog;
import io.github.luasgraphicss.minemod.compatibility.dispatch.catalog.CallDescription;
import io.github.luasgraphicss.minemod.compatibility.dispatch.catalog.CallFamily;
import io.github.luasgraphicss.minemod.compatibility.dispatch.table.DispatchTable;
import io.github.luasgraphicss.minemod.compatibility.provider.capability.ContextProfile;
import io.github.luasgraphicss.minemod.verification.scenario.simulation.SimulatedDispatch;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

public final class CapabilityMatrix {
    private static final String LABEL_FORMAT = "%-20s";
    private static final String CELL_FORMAT = "%22s";
    private static final String HEADER_LABEL = "family";
    private static final String TOTAL_LABEL = "total";

    private final Map<CallFamily, Map<OperationSupport, Integer>> counts = new EnumMap<>(CallFamily.class);

    private CapabilityMatrix() {}

    public static CapabilityMatrix of(CallCatalog catalog, ContextProfile profile) {
        CapabilityMatrix matrix = new CapabilityMatrix();
        DispatchTable table = SimulatedDispatch.table(catalog, profile);
        for (CallDescription description : catalog.descriptions()) {
            OperationSupport support = table.entry(description.function()).support();
            matrix.counts
                    .computeIfAbsent(description.family(), unused -> new EnumMap<>(OperationSupport.class))
                    .merge(support, 1, Integer::sum);
        }
        return matrix;
    }

    public int count(CallFamily family, OperationSupport support) {
        Map<OperationSupport, Integer> row = counts.get(family);
        return row == null ? 0 : row.getOrDefault(support, 0);
    }

    public int total(OperationSupport support) {
        return counts.keySet().stream().mapToInt(family -> count(family, support)).sum();
    }

    public List<String> rows() {
        List<String> rows = new ArrayList<>();
        rows.add(row(HEADER_LABEL, OperationSupport::description));
        for (CallFamily family : counts.keySet()) {
            rows.add(row(labelOf(family), support -> Integer.toString(count(family, support))));
        }
        rows.add(row(TOTAL_LABEL, support -> Integer.toString(total(support))));
        return rows;
    }

    private static String labelOf(CallFamily family) {
        return family.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    private static String row(String label, Function<OperationSupport, String> cell) {
        StringBuilder row = new StringBuilder(String.format(Locale.ROOT, LABEL_FORMAT, label));
        for (OperationSupport support : OperationSupport.values()) {
            row.append(String.format(Locale.ROOT, CELL_FORMAT, cell.apply(support)));
        }
        return row.toString();
    }
}
