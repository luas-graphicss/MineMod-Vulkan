package io.github.luasgraphicss.minemod.adapters.transformers.rewrite;

import io.github.luasgraphicss.minemod.adapters.transformers.callsite.CallSiteRedirect;
import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

final class RedirectingClassVisitor extends ClassVisitor {
    private final List<CallSiteRedirect> redirects;
    private final String hookOwner;
    private final MatchCounter matches;

    RedirectingClassVisitor(
            int api, ClassVisitor delegate, List<CallSiteRedirect> redirects, String hookOwner, MatchCounter matches) {
        super(api, delegate);
        this.redirects = redirects;
        this.hookOwner = hookOwner;
        this.matches = matches;
    }

    @Override
    public MethodVisitor visitMethod(
            int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor method = super.visitMethod(access, name, descriptor, signature, exceptions);
        List<CallSiteRedirect> withinMethod = new ArrayList<>();
        for (CallSiteRedirect redirect : redirects) {
            if (redirect.isWithin(name, descriptor)) {
                withinMethod.add(redirect);
            }
        }
        return withinMethod.isEmpty()
                ? method
                : new RedirectingMethodVisitor(api, method, withinMethod, hookOwner, matches);
    }
}
