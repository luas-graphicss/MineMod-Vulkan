package io.github.luasgraphicss.minemod.adapters.transformers.rewrite;

import io.github.luasgraphicss.minemod.adapters.transformers.callsite.CallSiteRedirect;
import java.util.List;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

final class RedirectingMethodVisitor extends MethodVisitor {
    private final List<CallSiteRedirect> redirects;
    private final String hookOwner;
    private final MatchCounter matches;

    RedirectingMethodVisitor(
            int api, MethodVisitor delegate, List<CallSiteRedirect> redirects, String hookOwner, MatchCounter matches) {
        super(api, delegate);
        this.redirects = redirects;
        this.hookOwner = hookOwner;
        this.matches = matches;
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        CallSiteRedirect redirect = opcode == Opcodes.INVOKESTATIC ? redirectOf(owner, name, descriptor) : null;
        if (redirect == null) {
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
            return;
        }
        matches.count(redirect);
        super.visitMethodInsn(Opcodes.INVOKESTATIC, hookOwner, name, descriptor, false);
    }

    private CallSiteRedirect redirectOf(String owner, String name, String descriptor) {
        for (CallSiteRedirect redirect : redirects) {
            if (redirect.redirects(owner, name, descriptor)) {
                return redirect;
            }
        }
        return null;
    }
}
