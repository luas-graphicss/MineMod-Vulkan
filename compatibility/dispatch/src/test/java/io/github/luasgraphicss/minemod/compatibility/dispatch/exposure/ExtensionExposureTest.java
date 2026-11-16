package io.github.luasgraphicss.minemod.compatibility.dispatch.exposure;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.luasgraphicss.minemod.compatibility.dispatch.catalog.CallCatalog;
import io.github.luasgraphicss.minemod.compatibility.dispatch.table.DispatchTable;
import io.github.luasgraphicss.minemod.compatibility.dispatch.table.FunctionSource;
import io.github.luasgraphicss.minemod.compatibility.dispatch.table.LibraryImage;
import io.github.luasgraphicss.minemod.compatibility.provider.capability.ContextProfile;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ExtensionExposureTest {
    private static final long PROVIDER_ADDRESS = 0x7000_0010L;
    private static final long THUNK_ADDRESS = 0x5000_0010L;
    private static final List<String> PROVIDER_EXTENSIONS = Arrays.asList(
            "GL_ARB_vertex_array_object", "GL_ARB_copy_buffer", "GL_ARB_texture_float", "GL_NV_gpu_shader5");

    private final CallCatalog catalog = CallCatalog.standard();
    private final Set<String> missingThunks = new HashSet<>();

    @Test
    void anApprovedExtensionIsExposedWhenAllItsFunctionsResolve() {
        assertEquals(
                new HashSet<>(Arrays.asList(
                        "GL_ARB_vertex_array_object", "GL_ARB_copy_buffer", "GL_ARB_texture_float")),
                exposed());
    }

    @Test
    void anExtensionIsHiddenWhenOneOfItsFunctionsIsWithheld() {
        missingThunks.add("glBindVertexArray");

        assertEquals(new HashSet<>(Arrays.asList("GL_ARB_copy_buffer", "GL_ARB_texture_float")), exposed());
    }

    @Test
    void anExtensionTheProviderLacksIsNeverInvented() {
        assertEquals(
                Collections.emptySet(),
                ExtensionExposure.exposed(Collections.<String>emptyList(), catalog.extensions(), table()));
    }

    private Set<String> exposed() {
        return ExtensionExposure.exposed(PROVIDER_EXTENSIONS, catalog.extensions(), table());
    }

    private DispatchTable table() {
        FunctionSource provider = function -> PROVIDER_ADDRESS;
        FunctionSource thunks = function -> missingThunks.contains(function) ? 0 : THUNK_ADDRESS;
        return new DispatchTable(
                catalog, ContextProfile.CORE, provider, LibraryImage.ofSegments(0x7000_0000L, 0x7100_0000L), thunks);
    }
}
