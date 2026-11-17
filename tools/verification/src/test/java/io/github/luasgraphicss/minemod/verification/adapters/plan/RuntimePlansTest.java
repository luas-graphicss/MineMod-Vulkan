package io.github.luasgraphicss.minemod.verification.adapters.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.adapters.transformers.release.ReleasePlan;
import io.github.luasgraphicss.minemod.verification.adapters.fixtures.PinnedWindow;
import io.github.luasgraphicss.minemod.verification.adapters.names.NameTable;
import io.github.luasgraphicss.minemod.verification.adapters.names.RuntimeNamespace;
import io.github.luasgraphicss.minemod.verification.adapters.names.TsrgNames.SrgNames;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RuntimePlansTest {
    @Test
    void writesTheOfficialPlanAsTheHooksWerePinned() {
        ReleasePlan plan = RuntimePlans.written(PinnedWindow.hooks(), RuntimeNamespace.OFFICIAL);

        assertEquals(PinnedWindow.RELEASE, plan.release());
        assertEquals(PinnedWindow.CLIENT_SHA1, plan.clientSha1());
        assertEquals(PinnedWindow.OFFICIAL_SWAP, enclosingMethodOf(plan));
    }

    @Test
    void writesTheMojangPlanFromTheNamesTheHooksCarry() {
        ReleasePlan plan = RuntimePlans.written(PinnedWindow.hooks(), RuntimeNamespace.MOJANG);

        assertEquals(PinnedWindow.MOJANG_SWAP, enclosingMethodOf(plan));
    }

    @Test
    void refusesAMojangPlanForAReleaseWithoutThoseNames() {
        IllegalArgumentException refusal = assertThrows(
                IllegalArgumentException.class,
                () -> RuntimePlans.written(PinnedWindow.hooksWithoutMojangNames(), RuntimeNamespace.MOJANG));

        assertTrue(refusal.getMessage().contains("swap-buffers"), refusal.getMessage());
    }

    @Test
    void refusesToWriteWhatIsReadFromAMappingFile() {
        assertThrows(
                IllegalArgumentException.class,
                () -> RuntimePlans.written(PinnedWindow.hooks(), RuntimeNamespace.INTERMEDIARY));
    }

    @Test
    void translatesThePlanToIntermediaryNames() {
        ReleasePlan plan = RuntimePlans.intermediary(PinnedWindow.hooks(), PinnedWindow.intermediaryNames());

        assertEquals(PinnedWindow.INTERMEDIARY_SWAP, enclosingMethodOf(plan));
    }

    @Test
    void keepsTheMojangClassWhenTheSrgFileNamesOnlyMembers() {
        NameTable members = new NameTable(Map.of(), Map.of(NameTable.methodKey("cfs", "a", "(Z)V"), "m_85435_"));

        ReleasePlan plan = RuntimePlans.srg(PinnedWindow.hooks(), new SrgNames(members, true));

        assertEquals("com/mojang/blaze3d/platform/Window.m_85435_(Z)V", enclosingMethodOf(plan));
    }

    @Test
    void translatesClassesTooWhenTheSrgFileNamesThem() {
        NameTable names = new NameTable(
                Map.of("cfs", "net/minecraft/client/MainWindow"),
                Map.of(NameTable.methodKey("cfs", "a", "(Z)V"), "func_216523_b"));

        ReleasePlan plan = RuntimePlans.srg(PinnedWindow.hooks(), new SrgNames(names, false));

        assertEquals("net/minecraft/client/MainWindow.func_216523_b(Z)V", enclosingMethodOf(plan));
    }

    private static String enclosingMethodOf(ReleasePlan plan) {
        assertEquals(1, plan.redirects().size());
        return plan.redirects().getFirst().point().target().toString();
    }
}
