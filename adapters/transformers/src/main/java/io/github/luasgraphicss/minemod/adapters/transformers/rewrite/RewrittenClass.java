package io.github.luasgraphicss.minemod.adapters.transformers.rewrite;

import io.github.luasgraphicss.minemod.adapters.transformers.application.PointApplication;
import java.util.Collections;
import java.util.List;

public final class RewrittenClass {
    private final byte[] classFile;
    private final List<PointApplication> applications;

    RewrittenClass(byte[] classFile, List<PointApplication> applications) {
        this.classFile = classFile;
        this.applications = Collections.unmodifiableList(applications);
    }

    public byte[] classFile() {
        return classFile.clone();
    }

    public List<PointApplication> applications() {
        return applications;
    }
}
