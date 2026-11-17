package io.github.luasgraphicss.minemod.verification.journey.instance;

import java.io.IOException;

public interface InstanceLauncher {
    InstanceExit launch(InstanceRequest request) throws IOException, InterruptedException;
}
