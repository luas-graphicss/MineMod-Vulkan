package io.github.luasgraphicss.minemod.verification.capability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.compatibility.contracts.support.OperationSupport;
import io.github.luasgraphicss.minemod.compatibility.dispatch.catalog.CallCatalog;
import io.github.luasgraphicss.minemod.compatibility.dispatch.catalog.CallFamily;
import io.github.luasgraphicss.minemod.compatibility.provider.capability.ContextProfile;
import java.util.List;
import org.junit.jupiter.api.Test;

class CapabilityMatrixTest {
    private final CallCatalog catalog = CallCatalog.standard();

    @Test
    void aCompatibilityContextServesEveryDescribedFunction() {
        CapabilityMatrix matrix = CapabilityMatrix.of(catalog, ContextProfile.COMPATIBILITY);

        assertEquals(catalog.descriptions().size(), matrix.total(OperationSupport.SUPPORTED));
        assertEquals(0, matrix.total(OperationSupport.OUT_OF_CONTRACT));
        assertEquals(0, matrix.total(OperationSupport.UNVERIFIED));
    }

    @Test
    void aCoreContextKeepsTheLegacyFamiliesOutsideTheContract() {
        CapabilityMatrix matrix = CapabilityMatrix.of(catalog, ContextProfile.CORE);

        for (CallFamily family : CallFamily.values()) {
            int supported = matrix.count(family, OperationSupport.SUPPORTED);
            int outside = matrix.count(family, OperationSupport.OUT_OF_CONTRACT);
            boolean withheld = supported == 0 && outside > 0;
            boolean served = supported > 0 && outside == 0;
            assertTrue(family.isLegacy() ? withheld : served, family.name());
        }
        assertEquals(
                catalog.descriptions().size(),
                matrix.total(OperationSupport.SUPPORTED) + matrix.total(OperationSupport.OUT_OF_CONTRACT));
    }

    @Test
    void theStaticTableNeverClaimsABridge() {
        for (ContextProfile profile : ContextProfile.values()) {
            assertEquals(0, CapabilityMatrix.of(catalog, profile).total(OperationSupport.BRIDGE_REQUIRED));
        }
    }

    @Test
    void rowsNameEveryClassOfSupportEveryFamilyAndTheTotals() {
        List<String> rows = CapabilityMatrix.of(catalog, ContextProfile.CORE).rows();

        assertEquals(CallFamily.values().length + 2, rows.size());
        for (OperationSupport support : OperationSupport.values()) {
            assertTrue(rows.get(0).contains(support.description()), rows.get(0));
        }
        assertTrue(rows.get(1).startsWith("immediate-mode"), rows.get(1));
        assertTrue(rows.get(rows.size() - 1).startsWith("total"), rows.get(rows.size() - 1));
    }
}
