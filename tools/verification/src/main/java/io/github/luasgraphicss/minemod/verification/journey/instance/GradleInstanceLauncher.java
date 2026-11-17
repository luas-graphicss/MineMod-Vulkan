package io.github.luasgraphicss.minemod.verification.journey.instance;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public final class GradleInstanceLauncher implements InstanceLauncher {
    private static final String LOG = "launch.log";
    private static final String WRAPPER = "gradlew";
    private static final String WINDOWS_WRAPPER = "gradlew.bat";

    @Override
    public InstanceExit launch(InstanceRequest request) throws IOException, InterruptedException {
        Path log = request.directory().resolve(LOG);
        Process process = new ProcessBuilder(commandOf(request))
                .directory(request.repositoryRoot().toFile())
                .redirectErrorStream(true)
                .redirectOutput(log.toFile())
                .start();
        try {
            boolean ended = process.waitFor(request.timeout().toMillis(), TimeUnit.MILLISECONDS);
            if (ended) {
                return new InstanceExit(process.exitValue(), false, log);
            }
        } finally {
            stop(process);
        }
        return new InstanceExit(process.waitFor(), true, log);
    }

    private static List<String> commandOf(InstanceRequest request) {
        List<String> command = new ArrayList<>(wrapperIn(request.repositoryRoot()));
        command.addAll(List.of(
                "--project-dir",
                request.driverBuild().toString(),
                request.instance().task(),
                "-Pjourney.plan=" + request.planFile(),
                "-Pjourney.output=" + request.directory(),
                "--no-daemon",
                "--console=plain"));
        request.candidateMod().ifPresent(mod -> command.add("-Pjourney.mod=" + mod));
        return command;
    }

    private static List<String> wrapperIn(Path repositoryRoot) {
        boolean isWindows = System.getProperty("os.name").toLowerCase(Locale.ROOT).startsWith("windows");
        return isWindows
                ? List.of(repositoryRoot.resolve(WINDOWS_WRAPPER).toString())
                : List.of("sh", repositoryRoot.resolve(WRAPPER).toString());
    }

    private static void stop(Process process) {
        if (process.isAlive()) {
            process.descendants().forEach(ProcessHandle::destroyForcibly);
            process.destroyForcibly();
        }
    }
}
