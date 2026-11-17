package io.github.luasgraphicss.minemod.adapters.transformers.rewrite;

import io.github.luasgraphicss.minemod.adapters.transformers.application.PointApplication;
import io.github.luasgraphicss.minemod.adapters.transformers.callsite.CallSiteRedirect;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

public final class CallSiteRewriter {
    private static final Pattern INTERNAL_NAME = Pattern.compile("[^;\\[./]+(?:/[^;\\[./]+)*");

    private final String hookOwner;
    private final Map<String, List<CallSiteRedirect>> redirectsByClass = new LinkedHashMap<>();

    public CallSiteRewriter(Collection<CallSiteRedirect> redirects, String hookOwner) {
        if (!INTERNAL_NAME.matcher(hookOwner).matches()) {
            throw new IllegalArgumentException("hook owner is not a JVM internal class name: " + hookOwner);
        }
        this.hookOwner = hookOwner;
        for (CallSiteRedirect redirect : redirects) {
            redirectsByClass
                    .computeIfAbsent(redirect.enclosingClass(), owner -> new ArrayList<>())
                    .add(redirect);
        }
    }

    public Set<String> rewrittenClasses() {
        return Collections.unmodifiableSet(redirectsByClass.keySet());
    }

    public boolean rewrites(String className) {
        return redirectsByClass.containsKey(className);
    }

    public RewrittenClass rewrite(String className, byte[] classFile) {
        List<CallSiteRedirect> redirects = redirectsByClass.get(className);
        if (redirects == null) {
            throw new IllegalArgumentException("no call is redirected in " + className);
        }
        ClassReader reader = new ClassReader(classFile);
        if (!reader.getClassName().equals(className)) {
            throw new IllegalArgumentException(
                    "expected the class file of " + className + ", found " + reader.getClassName());
        }

        MatchCounter matches = new MatchCounter(redirects);
        ClassWriter writer = new ClassWriter(reader, 0);
        reader.accept(
                new RedirectingClassVisitor(ClassFileVersion.asmApiOf(classFile), writer, redirects, hookOwner, matches),
                0);
        return new RewrittenClass(writer.toByteArray(), applicationsOf(redirects, matches));
    }

    private static List<PointApplication> applicationsOf(List<CallSiteRedirect> redirects, MatchCounter matches) {
        List<PointApplication> applications = new ArrayList<>(redirects.size());
        for (CallSiteRedirect redirect : redirects) {
            applications.add(new PointApplication(redirect.point().id(), matches.of(redirect)));
        }
        return applications;
    }
}
