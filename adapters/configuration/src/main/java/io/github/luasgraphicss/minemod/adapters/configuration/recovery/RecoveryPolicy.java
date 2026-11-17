package io.github.luasgraphicss.minemod.adapters.configuration.recovery;

import java.util.List;

public final class RecoveryPolicy {
    private RecoveryPolicy() {}

    public static RecoveryDecision decide(List<LaunchPhase> previousLaunches, int failedLaunchesBeforeSafeMode) {
        int failed = 0;
        for (int index = previousLaunches.size() - 1; index >= 0; index--) {
            if (previousLaunches.get(index).presentedAFrame()) {
                break;
            }
            failed++;
        }
        if (failed >= failedLaunchesBeforeSafeMode) {
            return new RecoveryDecision(true, "the last " + failed + " launch(es) ended before the first frame");
        }
        return new RecoveryDecision(false, failed + " launch(es) ended before the first frame, safe mode starts at "
                + failedLaunchesBeforeSafeMode);
    }
}
