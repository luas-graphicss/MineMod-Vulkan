package io.github.luasgraphicss.minemod.adapters.loaders.modlauncher;

import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.ITransformerVotingContext;
import cpw.mods.modlauncher.api.TransformerVoteResult;
import io.github.luasgraphicss.minemod.adapters.loaders.launch.apply.ClassNodeApplication;
import java.util.Collections;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;

final class PinnedClassTransformer implements ITransformer<ClassNode> {
    private final ClassNodeApplication application;
    private final String className;

    PinnedClassTransformer(ClassNodeApplication application, String className) {
        this.application = application;
        this.className = className;
    }

    @Override
    public ClassNode transform(ClassNode input, ITransformerVotingContext context) {
        application.rewrite(input);
        return input;
    }

    @Override
    public TransformerVoteResult castVote(ITransformerVotingContext context) {
        return TransformerVoteResult.YES;
    }

    @Override
    public Set<Target> targets() {
        return Collections.singleton(Target.targetClass(className));
    }
}
