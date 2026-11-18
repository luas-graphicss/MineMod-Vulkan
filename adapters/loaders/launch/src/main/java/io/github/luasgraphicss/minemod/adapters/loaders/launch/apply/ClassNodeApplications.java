package io.github.luasgraphicss.minemod.adapters.loaders.launch.apply;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;

public final class ClassNodeApplications {
    private final List<ClassNodeApplication> applications;

    public ClassNodeApplications(List<ClassNodeApplication> applications) {
        if (applications.isEmpty()) {
            throw new IllegalArgumentException("no plan is applied to the classes of the game");
        }
        this.applications = Collections.unmodifiableList(new ArrayList<>(applications));
    }

    public Set<String> rewrittenClasses() {
        Set<String> classes = new LinkedHashSet<>();
        for (ClassNodeApplication application : applications) {
            classes.addAll(application.rewrittenClasses());
        }
        return Collections.unmodifiableSet(classes);
    }

    public void rewrite(ClassNode classNode) {
        for (ClassNodeApplication application : applications) {
            if (application.rewrittenClasses().contains(classNode.name)) {
                application.rewrite(classNode);
            }
        }
    }
}
