package io.github.luasgraphicss.minemod.compatibility.translation.section;

import io.github.luasgraphicss.minemod.compatibility.contracts.support.OperationSupport;
import io.github.luasgraphicss.minemod.compatibility.state.reconcile.StateReconciler;
import io.github.luasgraphicss.minemod.compatibility.state.submission.SubmissionCheckpoints;
import io.github.luasgraphicss.minemod.compatibility.translation.legacy.OpenConstructs;
import io.github.luasgraphicss.minemod.compatibility.translation.provider.ProviderCalls;
import io.github.luasgraphicss.minemod.compatibility.translation.query.QuerySpans;
import io.github.luasgraphicss.minemod.compatibility.translation.report.OperationReporter;
import java.util.List;
import java.util.Objects;

public final class MixedSections {
    private final StateReconciler reconciler;
    private final SubmissionCheckpoints checkpoints;
    private final QuerySpans spans;
    private final OpenConstructs constructs;
    private final ProviderCalls provider;
    private final OperationReporter reporter;

    public MixedSections(
            StateReconciler reconciler,
            SubmissionCheckpoints checkpoints,
            QuerySpans spans,
            OpenConstructs constructs,
            ProviderCalls provider,
            OperationReporter reporter) {
        this.reconciler = Objects.requireNonNull(reconciler, "reconciler");
        this.checkpoints = Objects.requireNonNull(checkpoints, "checkpoints");
        this.spans = Objects.requireNonNull(spans, "spans");
        this.constructs = Objects.requireNonNull(constructs, "constructs");
        this.provider = Objects.requireNonNull(provider, "provider");
        this.reporter = Objects.requireNonNull(reporter, "reporter");
    }

    public int enterCallback() {
        constructs.beginSection();
        return reconciler.enterSection();
    }

    public List<String> exitCallback() {
        List<String> leftOpen = constructs.closeSection(provider);
        for (String finding : leftOpen) {
            reporter.report(finding, OperationSupport.OUT_OF_CONTRACT);
        }
        reconciler.exitSection();
        return leftOpen;
    }

    public void beforeNativePass() {
        requireNoOpenCallback();
        checkpoints.beforeNativePass();
        spans.nativeWorkRecorded();
    }

    public void present() {
        requireNoOpenCallback();
        checkpoints.present();
        spans.nativeWorkRecorded();
    }

    private void requireNoOpenCallback() {
        if (reconciler.inSection()) {
            throw new IllegalStateException("a mod callback is still inside its OpenGL section");
        }
    }
}
