package io.github.luasgraphicss.minemod.adapters.loaders.modlauncher11;

import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.ITransformerVotingContext;
import cpw.mods.modlauncher.api.TargetType;
import cpw.mods.modlauncher.api.TransformerVoteResult;
import io.github.luasgraphicss.minemod.adapters.loaders.launch.apply.ClassNodeApplication;
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
    public Set<Target<ClassNode>> targets() {
        return Set.of(Target.targetClass(className));
    }

    @Override
    public TargetType<ClassNode> getTargetType() {
        return TargetType.CLASS;
    }
}
