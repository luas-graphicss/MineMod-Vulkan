package io.github.luasgraphicss.minemod.adapters.loaders.modlauncher.frame;

import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.ITransformerVotingContext;
import cpw.mods.modlauncher.api.TransformerVoteResult;
import io.github.luasgraphicss.minemod.adapters.loaders.launch.apply.ClassNodeApplications;
import java.util.Collections;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;

final class PinnedFrameTransformer implements ITransformer<ClassNode> {
    private final ClassNodeApplications applications;
    private final String className;

    PinnedFrameTransformer(ClassNodeApplications applications, String className) {
        this.applications = applications;
        this.className = className;
    }

    @Override
    public ClassNode transform(ClassNode input, ITransformerVotingContext context) {
        applications.rewrite(input);
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
