package io.github.luasgraphicss.minemod.adapters.loaders.launch.apply;

import io.github.luasgraphicss.minemod.adapters.transformers.application.PointApplication;
import io.github.luasgraphicss.minemod.adapters.transformers.callsite.CallSiteRedirect;
import io.github.luasgraphicss.minemod.adapters.transformers.release.ReleasePlan;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public final class ClassNodeApplication {
    private final ReleasePlan plan;
    private final String hookOwner;
    private final Map<String, List<CallSiteRedirect>> redirectsByClass = new LinkedHashMap<>();

    public ClassNodeApplication(ReleasePlan plan, String hookOwner) {
        this.plan = plan;
        this.hookOwner = hookOwner;
        for (CallSiteRedirect redirect : plan.redirects()) {
            List<CallSiteRedirect> ofClass = redirectsByClass.get(redirect.enclosingClass());
            if (ofClass == null) {
                ofClass = new ArrayList<>();
                redirectsByClass.put(redirect.enclosingClass(), ofClass);
            }
            ofClass.add(redirect);
        }
    }

    public Set<String> rewrittenClasses() {
        return Collections.unmodifiableSet(redirectsByClass.keySet());
    }

    public void rewrite(ClassNode classNode) {
        List<CallSiteRedirect> redirects = redirectsByClass.get(classNode.name);
        if (redirects == null) {
            throw new IllegalArgumentException("no call is redirected in " + classNode.name);
        }
        Map<CallSiteRedirect, Integer> matches = new IdentityHashMap<>();
        for (MethodNode method : classNode.methods) {
            redirectCallsOf(method, redirects, matches);
        }
        List<PointApplication> applications = new ArrayList<>(redirects.size());
        for (CallSiteRedirect redirect : redirects) {
            Integer matched = matches.get(redirect);
            applications.add(new PointApplication(redirect.point().id(), matched == null ? 0 : matched));
        }
        ClassGate.requireExactMatches(plan, classNode.name, applications);
    }

    private void redirectCallsOf(
            MethodNode method, List<CallSiteRedirect> redirects, Map<CallSiteRedirect, Integer> matches) {
        for (AbstractInsnNode instruction = method.instructions.getFirst();
                instruction != null;
                instruction = instruction.getNext()) {
            if (instruction.getOpcode() != Opcodes.INVOKESTATIC) {
                continue;
            }
            MethodInsnNode call = (MethodInsnNode) instruction;
            CallSiteRedirect redirect = redirectOf(method, call, redirects);
            if (redirect != null) {
                call.owner = hookOwner;
                call.itf = false;
                Integer matched = matches.get(redirect);
                matches.put(redirect, matched == null ? 1 : matched + 1);
            }
        }
    }

    private static CallSiteRedirect redirectOf(
            MethodNode method, MethodInsnNode call, List<CallSiteRedirect> redirects) {
        for (CallSiteRedirect redirect : redirects) {
            if (redirect.isWithin(method.name, method.desc) && redirect.redirects(call.owner, call.name, call.desc)) {
                return redirect;
            }
        }
        return null;
    }
}
