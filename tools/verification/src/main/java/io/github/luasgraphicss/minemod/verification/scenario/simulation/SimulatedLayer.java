package io.github.luasgraphicss.minemod.verification.scenario.simulation;

import io.github.luasgraphicss.minemod.compatibility.contexts.CompatibilityContext;
import io.github.luasgraphicss.minemod.compatibility.contexts.ContextRegistry;
import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import io.github.luasgraphicss.minemod.compatibility.contracts.report.CallSiteLocator;
import io.github.luasgraphicss.minemod.compatibility.contracts.report.ReportLog;
import io.github.luasgraphicss.minemod.compatibility.dispatch.catalog.CallCatalog;
import io.github.luasgraphicss.minemod.compatibility.dispatch.catalog.CallDescription;
import io.github.luasgraphicss.minemod.compatibility.interop.handoff.SharedAttachmentHandoff;
import io.github.luasgraphicss.minemod.compatibility.provider.capability.ContextProfile;
import io.github.luasgraphicss.minemod.compatibility.resources.framebuffer.DefaultFramebufferBuffers;
import io.github.luasgraphicss.minemod.compatibility.resources.framebuffer.FramebufferBindings;
import io.github.luasgraphicss.minemod.compatibility.resources.names.ContextNames;
import io.github.luasgraphicss.minemod.compatibility.state.error.ErrorLedger;
import io.github.luasgraphicss.minemod.compatibility.state.reconcile.StateReconciler;
import io.github.luasgraphicss.minemod.compatibility.state.submission.AttachmentOwner;
import io.github.luasgraphicss.minemod.compatibility.state.submission.FlushLog;
import io.github.luasgraphicss.minemod.compatibility.state.submission.SubmissionCheckpoints;
import io.github.luasgraphicss.minemod.compatibility.translation.call.CallTranslator;
import io.github.luasgraphicss.minemod.compatibility.translation.framebuffer.VirtualFramebufferCalls;
import io.github.luasgraphicss.minemod.compatibility.translation.legacy.DisplayListCalls;
import io.github.luasgraphicss.minemod.compatibility.translation.legacy.DisplayListNames;
import io.github.luasgraphicss.minemod.compatibility.translation.legacy.OpenConstructs;
import io.github.luasgraphicss.minemod.compatibility.translation.names.NameArguments;
import io.github.luasgraphicss.minemod.compatibility.translation.names.ObjectNames;
import io.github.luasgraphicss.minemod.compatibility.translation.query.QueryCalls;
import io.github.luasgraphicss.minemod.compatibility.translation.query.QuerySpans;
import io.github.luasgraphicss.minemod.compatibility.translation.report.OperationReporter;
import io.github.luasgraphicss.minemod.compatibility.translation.section.MixedSections;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public final class SimulatedLayer implements GlDriver {
    private static final int NAMES_PER_KIND = 512;
    private static final int REPORT_CAPACITY = 64;
    private static final int FLUSH_HISTORY = 64;
    private static final String OWN_PACKAGES = "io.github.luasgraphicss.minemod.";
    private static final String ENABLE = "glEnable";
    private static final String DISABLE = "glDisable";

    private final CallCatalog catalog = CallCatalog.standard();
    private final Timeline timeline = new Timeline();
    private final SimulatedSharing sharing = new SimulatedSharing(timeline);
    private final SimulatedProvider provider;
    private final ContextRegistry contexts = new ContextRegistry(new SimulatedContexts(), NAMES_PER_KIND);
    private final CompatibilityContext context = contexts.create();
    private final ReportLog reports = new ReportLog(REPORT_CAPACITY);
    private final FlushLog flushes = new FlushLog(FLUSH_HISTORY);
    private final SharedAttachmentHandoff handoff = new SharedAttachmentHandoff(sharing);
    private final SubmissionCheckpoints checkpoints = new SubmissionCheckpoints(handoff, flushes);
    private final OpenConstructs constructs = new OpenConstructs();
    private final long[] wrapperCache = new long[TrackedState.values().length];
    private final List<String> ownershipViolations = new ArrayList<>();
    private final ErrorLedger errors;
    private final StateReconciler reconciler;
    private final ObjectNames names;
    private final DisplayListNames lists;
    private final DisplayListCalls listCalls;
    private final QuerySpans spans;
    private final QueryCalls queries;
    private final VirtualFramebufferCalls framebuffers;
    private final CallTranslator translator;
    private final MixedSections sections;
    private long frame;

    public SimulatedLayer(ContextProfile profile) {
        provider = new SimulatedProvider(profile, timeline, sharing::owner);
        contexts.makeCurrent(context);
        errors = new ErrorLedger(provider);
        OperationReporter reporter = new OperationReporter(
                reports, new CallSiteLocator(List.of(OWN_PACKAGES), className -> Optional.empty()));
        Supplier<ContextNames> currentNames = () -> contexts.requireCurrent().names();
        names = new ObjectNames(currentNames, provider, errors, () -> frame, profile);
        lists = new DisplayListNames(currentNames, provider, errors, () -> frame);
        listCalls = new DisplayListCalls(lists, constructs, checkpoints, provider);
        spans = new QuerySpans(reporter, errors);
        queries = new QueryCalls(spans, names, checkpoints, provider);
        reconciler = new StateReconciler(
                new long[wrapperCache.length],
                TrackedState.policies(),
                this::applyToProvider,
                (slot, value) -> wrapperCache[slot] = value);
        framebuffers = new VirtualFramebufferCalls(
                new FramebufferBindings(context.names().table(ObjectKind.FRAMEBUFFER), provider.adoptSceneFramebuffer()),
                new DefaultFramebufferBuffers(),
                names,
                catalog,
                provider,
                errors,
                reporter);
        translator = new CallTranslator(
                catalog, new NameArguments(names), checkpoints, constructs, List.of(this::followDirectState), provider);
        sections = new MixedSections(reconciler, checkpoints, spans, constructs, provider, reporter);
    }

    public void beginFrame() {
        frame++;
        checkpoints.beginFrame(frame);
    }

    public void nativePass(String pass) {
        sections.beforeNativePass();
        recordNativeWork(pass);
    }

    public void present() {
        sections.present();
        recordNativeWork("present");
        for (ObjectKind kind : ObjectKind.values()) {
            context.names().table(kind).collect(frame, provider);
        }
    }

    public int enterCallback() {
        return sections.enterCallback();
    }

    public List<String> exitCallback() {
        return sections.exitCallback();
    }

    public void setThroughWrapper(TrackedState state, long value) {
        wrapperCache[state.ordinal()] = value;
        reconciler.setSemantic(state.ordinal(), value);
    }

    public long wrapperValue(TrackedState state) {
        return wrapperCache[state.ordinal()];
    }

    @Override
    public long call(String function, long... arguments) {
        CallDescription description = catalog.describe(function)
                .orElseThrow(() -> new IllegalArgumentException(function + " is not described"));
        return switch (description.interception()) {
            case FORWARD -> provider.invoke(function, arguments);
            case TRANSLATE -> translator.call(function, arguments);
            case FRAMEBUFFER -> framebufferCall(description.function(), function, arguments);
            case SPANS -> spanCall(description.function(), function, arguments);
            default -> throw new IllegalArgumentException("the simulated layer does not route " + function);
        };
    }

    @Override
    public int[] generate(ObjectKind kind, int count) {
        return names.generate(kind, count);
    }

    @Override
    public void delete(ObjectKind kind, int... deleted) {
        switch (kind) {
            case FRAMEBUFFER -> framebuffers.delete(deleted);
            case QUERY -> queries.delete(deleted);
            default -> names.delete(kind, deleted);
        }
    }

    @Override
    public boolean isObject(ObjectKind kind, int name) {
        return names.isLive(kind, name);
    }

    @Override
    public int generateLists(int range) {
        return lists.generate(range);
    }

    @Override
    public void deleteLists(int first, int range) {
        lists.delete(first, range);
    }

    @Override
    public void setListBase(int base) {
        lists.setBase(base);
    }

    @Override
    public void callLists(int... offsets) {
        listCalls.callLists(offsets);
    }

    @Override
    public int takeError() {
        return errors.take();
    }

    @Override
    public List<String> providerCalls() {
        return timeline.from(Timeline.Source.PROVIDER);
    }

    public Timeline timeline() {
        return timeline;
    }

    public SimulatedProvider provider() {
        return provider;
    }

    public FlushLog flushes() {
        return flushes;
    }

    public SharedAttachmentHandoff handoff() {
        return handoff;
    }

    public ReportLog reports() {
        return reports;
    }

    public AttachmentOwner attachmentOwner() {
        return sharing.owner();
    }

    public List<String> ownershipViolations() {
        List<String> violations = new ArrayList<>(ownershipViolations);
        violations.addAll(provider.ownershipViolations());
        return violations;
    }

    private void recordNativeWork(String work) {
        if (sharing.owner() != AttachmentOwner.RENDERER) {
            ownershipViolations.add(work + " ran while the provider owned the attachments");
        }
        timeline.add(Timeline.Source.NATIVE, work);
    }

    private long framebufferCall(String coreFunction, String function, long[] arguments) {
        switch (coreFunction) {
            case "glBindFramebuffer" -> framebuffers.bind((int) arguments[0], (int) arguments[1]);
            case "glDrawBuffer" -> framebuffers.drawBuffer((int) arguments[0]);
            case "glReadBuffer" -> framebuffers.readBuffer((int) arguments[0]);
            default -> framebuffers.attach(function, arguments);
        }
        return 0;
    }

    private long spanCall(String coreFunction, String function, long[] arguments) {
        switch (coreFunction) {
            case "glBeginQuery" -> queries.begin(function, (int) arguments[0], (int) arguments[1]);
            case "glEndQuery" -> queries.end(function, (int) arguments[0]);
            case "glBeginTransformFeedback" -> queries.beginTransformFeedback(function, (int) arguments[0]);
            case "glEndTransformFeedback" -> queries.endTransformFeedback(function);
            default -> {
                return queries.result(function, arguments);
            }
        }
        return 0;
    }

    private void applyToProvider(int slot, long value) {
        TrackedState state = TrackedState.values()[slot];
        provider.invoke(value == TrackedState.ENABLED ? ENABLE : DISABLE, new long[] {state.capability()});
    }

    private void followDirectState(String function, long[] arguments) {
        if (!function.equals(ENABLE) && !function.equals(DISABLE)) {
            return;
        }
        long value = function.equals(ENABLE) ? TrackedState.ENABLED : TrackedState.DISABLED;
        TrackedState.ofCapability(arguments[0]).ifPresent(state -> reconciler.observeDirect(state.ordinal(), value));
    }
}
