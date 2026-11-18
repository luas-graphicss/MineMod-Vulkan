package io.github.luasgraphicss.minemod.verification.machines.probe;

import io.github.luasgraphicss.minemod.verification.machines.model.ReferenceMachine.HostSystem;
import io.github.luasgraphicss.minemod.verification.machines.model.ReferenceMachine.SystemFamily;
import java.io.IOException;

public final class HostSystems {
    private static final String LINUX = "Linux";
    private static final String WINDOWS_PREFIX = "Windows";

    private HostSystems() {}

    public static HostSystem current() throws IOException {
        String name = System.getProperty("os.name");
        String description = name + " " + System.getProperty("os.version") + ", " + System.getProperty("os.arch");
        return new HostSystem(familyOf(name), description);
    }

    public static HostSystem currentDescribedAs(String description) throws IOException {
        return new HostSystem(familyOf(System.getProperty("os.name")), description);
    }

    static SystemFamily familyOf(String systemName) throws IOException {
        if (systemName.equals(LINUX)) {
            return SystemFamily.LINUX;
        }
        if (systemName.startsWith(WINDOWS_PREFIX)) {
            return SystemFamily.WINDOWS;
        }
        throw new IOException("reference machines run Linux or Windows, not " + systemName);
    }
}
