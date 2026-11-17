package io.github.luasgraphicss.minemod.journey.driver;

import io.github.luasgraphicss.minemod.journey.plan.JourneyPlan;
import io.github.luasgraphicss.minemod.journey.plan.JourneyPlanFile;
import io.github.luasgraphicss.minemod.journey.plan.PlanStep;
import io.github.luasgraphicss.minemod.journey.record.JourneyRecord;
import io.github.luasgraphicss.minemod.journey.record.JourneyRecordFile;
import io.github.luasgraphicss.minemod.journey.record.StepOutcome;
import io.github.luasgraphicss.minemod.journey.step.StepAction;
import io.github.luasgraphicss.minemod.journey.step.StepActions;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

public final class JourneyDriver {
    private static final String THREAD_NAME = "minemod-journey";
    private static final String PROVIDER_PROPERTY = "minemod.openGlProvider";
    private static final long POLL_MILLISECONDS = 50;

    private final JourneyPlan plan;
    private final JourneyRecord record;
    private final JourneyRecordFile recordFile;
    private final StepActions actions;
    private final ClientThread client;

    private JourneyDriver(JourneyPlanFile planFile, Path output, Minecraft minecraft) {
        this.plan = planFile.plan();
        this.record = new JourneyRecord(
                plan.name(), planFile.sha256(), loadedMods(), System.getProperty(PROVIDER_PROPERTY));
        this.recordFile = new JourneyRecordFile(output);
        this.actions = new StepActions(plan, record, output);
        this.client = new ClientThread(minecraft);
    }

    public static void start(Path planFile, Path output) {
        JourneyDriver driver = new JourneyDriver(JourneyPlanFile.read(planFile), output, Minecraft.getInstance());
        Thread thread = new Thread(driver::drive, THREAD_NAME);
        thread.setDaemon(true);
        thread.start();
    }

    private void drive() {
        try {
            boolean completed = performSteps();
            if (completed) {
                record.finish();
            }
            recordFile.write(record);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } finally {
            client.stopTheGame();
        }
    }

    private boolean performSteps() throws InterruptedException {
        List<PlanStep> steps = plan.steps();
        for (int index = 0; index < steps.size(); index++) {
            StepOutcome outcome = perform(index, steps.get(index));
            record.add(outcome);
            recordFile.write(record);
            if (!outcome.isCompleted()) {
                return false;
            }
        }
        return true;
    }

    private StepOutcome perform(int index, PlanStep step) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(plan.stepTimeoutSeconds());
        try {
            StepAction action = actions.of(step);
            client.run(action::begin);
            while (!client.ask(action::isDone)) {
                if (System.nanoTime() > deadline) {
                    return StepOutcome.timedOut(index, step, plan.stepTimeoutSeconds());
                }
                Thread.sleep(POLL_MILLISECONDS);
            }
            return StepOutcome.completed(index, step);
        } catch (CompletionException failure) {
            return StepOutcome.failed(index, step, String.valueOf(failure.getCause()));
        } catch (RuntimeException failure) {
            return StepOutcome.failed(index, step, failure.toString());
        }
    }

    private static List<String> loadedMods() {
        return FabricLoader.getInstance().getAllMods().stream()
                .map(mod -> mod.getMetadata().getId() + " " + mod.getMetadata().getVersion().getFriendlyString())
                .sorted()
                .toList();
    }
}
