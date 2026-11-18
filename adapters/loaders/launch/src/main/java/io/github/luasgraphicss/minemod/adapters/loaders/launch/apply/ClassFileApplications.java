package io.github.luasgraphicss.minemod.adapters.loaders.launch.apply;

import io.github.luasgraphicss.minemod.adapters.transformers.preflight.ClassSource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ClassFileApplications {
    private final List<ClassFileApplication> applications;

    public ClassFileApplications(List<ClassFileApplication> applications) {
        if (applications.isEmpty()) {
            throw new IllegalArgumentException("no plan is applied to the classes of the game");
        }
        this.applications = Collections.unmodifiableList(new ArrayList<>(applications));
    }

    public boolean rewrites(String className) {
        for (ClassFileApplication application : applications) {
            if (application.rewrites(className)) {
                return true;
            }
        }
        return false;
    }

    public byte[] rewrite(String className, byte[] classFile) {
        byte[] rewritten = classFile;
        for (ClassFileApplication application : applications) {
            if (application.rewrites(className)) {
                rewritten = application.rewrite(className, rewritten);
            }
        }
        return rewritten;
    }

    public void requireApplicableTo(ClassSource classes) throws IOException {
        for (ClassFileApplication application : applications) {
            application.requireApplicableTo(classes);
        }
    }
}
