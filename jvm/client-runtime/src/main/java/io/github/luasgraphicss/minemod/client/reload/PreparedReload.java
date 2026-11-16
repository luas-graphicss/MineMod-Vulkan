package io.github.luasgraphicss.minemod.client.reload;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PreparedReload {
    private final ResourceReload owner;
    private final long request;
    private final List<PreparedProgram> programs;
    private final List<String> problems;

    PreparedReload(ResourceReload owner, long request, List<PreparedProgram> programs, List<String> problems) {
        this.owner = owner;
        this.request = request;
        this.programs = Collections.unmodifiableList(new ArrayList<>(programs));
        this.problems = Collections.unmodifiableList(new ArrayList<>(problems));
    }

    public List<PreparedProgram> programs() {
        return programs;
    }

    public List<String> problems() {
        return problems;
    }

    boolean isRequest(ResourceReload reload, long latestRequest) {
        return owner == reload && request == latestRequest;
    }
}
