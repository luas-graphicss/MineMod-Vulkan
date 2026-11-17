package io.github.luasgraphicss.minemod.verification.machines.probe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.luasgraphicss.minemod.verification.machines.model.ReferenceMachine.HostSystem;
import io.github.luasgraphicss.minemod.verification.machines.model.ReferenceMachine.SystemFamily;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class HostSystemsTest {
    @Test
    void tellsLinuxFromEveryWindowsRelease() throws IOException {
        assertEquals(SystemFamily.LINUX, HostSystems.familyOf("Linux"));
        assertEquals(SystemFamily.WINDOWS, HostSystems.familyOf("Windows 11"));
        assertEquals(SystemFamily.WINDOWS, HostSystems.familyOf("Windows Server 2025"));
    }

    @Test
    void refusesASystemThatIsNeither() {
        IOException refused = assertThrows(IOException.class, () -> HostSystems.familyOf("Mac OS X"));

        assertEquals("reference machines run Linux or Windows, not Mac OS X", refused.getMessage());
    }

    @Test
    void keepsTheDescriptionTheOperatorGave() throws IOException {
        HostSystem current = HostSystems.current();

        HostSystem described = HostSystems.currentDescribedAs("Fedora 41, x86-64");

        assertEquals(new HostSystem(current.family(), "Fedora 41, x86-64"), described);
    }
}
