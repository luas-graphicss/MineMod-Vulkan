package io.github.luasgraphicss.minemod.verification.scenario.bootstrap;

import io.github.luasgraphicss.minemod.adapters.transformers.callsite.CallSiteRedirect;
import io.github.luasgraphicss.minemod.verification.hooks.cache.PinnedDownloads;
import io.github.luasgraphicss.minemod.verification.hooks.catalog.LibraryCall;
import io.github.luasgraphicss.minemod.verification.hooks.catalog.PresentationCalls;
import io.github.luasgraphicss.minemod.verification.hooks.model.PresentationHooks;
import io.github.luasgraphicss.minemod.verification.hooks.model.ReleaseHooks;
import io.github.luasgraphicss.minemod.verification.hooks.plan.ReleasePlans;
import io.github.luasgraphicss.minemod.verification.hooks.scan.CallSiteScanner;
import io.github.luasgraphicss.minemod.verification.hooks.storage.PresentationHooksFile;
import io.github.luasgraphicss.minemod.verification.matrix.model.AdapterBuild;
import io.github.luasgraphicss.minemod.verification.matrix.plan.AdapterBuildPlanner;
import io.github.luasgraphicss.minemod.verification.scenario.RepositoryScenario;
import io.github.luasgraphicss.minemod.verification.scenario.ScenarioEnvironment;
import io.github.luasgraphicss.minemod.verification.scenario.ScenarioLog;
import io.github.luasgraphicss.minemod.verification.scenario.ScenarioReport;
import io.github.luasgraphicss.minemod.verification.scenario.bootstrap.install.LauncherInstall;
import io.github.luasgraphicss.minemod.verification.scenario.bootstrap.install.LauncherInstallRules;
import io.github.luasgraphicss.minemod.verification.scenario.bootstrap.install.LauncherProfile.Library;
import io.github.luasgraphicss.minemod.verification.scenario.bootstrap.launch.AgentLaunch;
import io.github.luasgraphicss.minemod.verification.scenario.matrix.HookOwners;
import io.github.luasgraphicss.minemod.verification.scenario.transformers.PinnedSites;
import io.github.luasgraphicss.minemod.verification.targets.model.LoaderTarget;
import io.github.luasgraphicss.minemod.verification.targets.model.ReleaseTarget;
import io.github.luasgraphicss.minemod.verification.targets.model.TargetManifest;
import io.github.luasgraphicss.minemod.verification.targets.storage.TargetManifestFile;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;
import org.objectweb.asm.ClassReader;

public final class ClientBootstrapScenario implements RepositoryScenario {
    private static final int SHA1_PREFIX = 12;
    private static final String SCRATCH = "client-bootstrap";
    private static final String ACCEPTED = "accepted";
    private static final String REFUSED = "refused";
    private static final String REFUSAL = "is not one of the supported releases";

    @Override
    public String name() {
        return "client-bootstrap";
    }

    @Override
    public ScenarioReport run(ScenarioEnvironment environment) throws IOException, InterruptedException {
        ScenarioLog log = new ScenarioLog();
        TargetManifest targets = TargetManifestFile.inRepository(environment.repositoryRoot()).read();
        PresentationHooks hooks = PresentationHooksFile.inRepository(environment.repositoryRoot()).read();
        List<AdapterBuild> builds = AdapterBuildPlanner.plan(targets).stream()
                .filter(build -> build.loader() == LoaderTarget.Kind.CLIENT_BOOTSTRAP)
                .toList();
        log.expect(!builds.isEmpty(), "at least one release that starts without a loader");
        try (PinnedDownloads clients = new PinnedDownloads(environment.cacheDirectory())) {
            for (AdapterBuild build : builds) {
                Optional<LauncherInstall> install =
                        LauncherInstall.builtBy(environment.repositoryRoot(), build.loaderAdapter());
                if (install.isEmpty()) {
                    log.fail(build.release() + ": " + build.loaderAdapter()
                            + " has no launcher install; build it with \"gradlew "
                            + gradlePathOf(build.loaderAdapter()) + ":launcherInstall\"");
                    continue;
                }
                ReleaseTarget release = releaseOf(targets, build.release());
                ReleaseTarget other = targets.releases().stream()
                        .filter(candidate -> !candidate.id().equals(release.id()))
                        .findFirst()
                        .orElseThrow();
                verify(
                        environment.cacheDirectory(),
                        install.get(),
                        hooks.of(release.id()).orElseThrow(),
                        new Client(release, clients.clientOf(release)),
                        new Client(other, clients.clientOf(other)),
                        log);
            }
        }
        return log.report();
    }

    private static void verify(
            Path cacheDirectory, LauncherInstall install, ReleaseHooks hooks, Client pinned, Client other, ScenarioLog log)
            throws IOException, InterruptedException {
        ReleaseTarget release = pinned.release();
        String subject = release.id() + ": ";
        List<String> violations = LauncherInstallRules.findViolations(install, release);
        violations.forEach(violation -> log.fail(subject + violation));
        Optional<List<Path>> libraries = librariesOf(install, subject, log);
        if (!violations.isEmpty() || libraries.isEmpty()) {
            return;
        }
        log.observe(subject + "profile " + install.profile().id() + " inherits " + release.id() + ", keeps "
                + install.profile().mainClass() + " and adds one agent with the pinned libraries "
                + install.profile().libraries().stream().map(Library::name).toList()
                + "; the install holds nothing of the game");

        Path scratch = emptied(cacheDirectory.resolve(SCRATCH).resolve(release.id()));
        Set<String> hookedClasses = hookedClassesOf(hooks);
        String clientSha1 = PinnedDownloads.sha1Of(pinned.jar());
        AgentLaunch.Outcome accepted = AgentLaunch.run(
                install.jvmArguments(),
                classPathOf(pinned.jar(), libraries.get()),
                hookedClasses,
                scratch.resolve(ACCEPTED));
        if (accepted.exitCode() != 0) {
            log.fail(subject + "the JVM with the agent ended with " + accepted.exitCode() + ": " + accepted.output());
            return;
        }
        Map<String, Integer> redirected = redirectedSitesOf(accepted, hooks, hookedClasses, subject, log);
        log.expectEqual(PinnedSites.pinnedBy(hooks), redirected, subject + "calls the agent redirected in memory");
        log.expectEqual(
                clientSha1, PinnedDownloads.sha1Of(pinned.jar()), subject + "SHA-1 of the client after the run");
        log.observe(subject + "the agent redirected the " + hooks.callSites() + " pinned calls of client "
                + clientSha1.substring(0, SHA1_PREFIX)
                + " in the classes a child JVM defined; the client on disk kept its SHA-1");

        AgentLaunch.Outcome refused = AgentLaunch.run(
                install.jvmArguments(),
                classPathOf(other.jar(), libraries.get()),
                hookedClasses,
                scratch.resolve(REFUSED));
        log.expect(
                refused.exitCode() != 0 && refused.output().contains(REFUSAL) && refused.definedClasses().isEmpty(),
                subject + "the agent to stop the JVM before the client of " + other.release().id()
                        + " starts, found exit code "
                        + refused.exitCode() + ": " + refused.output());
        log.observe(subject + "the agent refused the client of " + other.release().id()
                + " before any of its classes was defined");
    }

    private static Map<String, Integer> redirectedSitesOf(
            AgentLaunch.Outcome launch, ReleaseHooks hooks, Set<String> hookedClasses, String subject, ScenarioLog log) {
        List<LibraryCall> libraryCalls = PresentationCalls.of(hooks.windowing());
        String hookOwner = HookOwners.of(hooks.windowing());
        List<LibraryCall> hookCalls = libraryCalls.stream()
                .map(call -> new LibraryCall(call.hook(), hookOwner, call.name(), call.descriptor()))
                .toList();
        Map<String, Integer> redirected = new TreeMap<>();
        for (String className : hookedClasses) {
            byte[] defined = launch.definedClasses().get(className);
            if (defined == null) {
                log.fail(subject + "the child JVM never defined " + className);
                continue;
            }
            if (!new CallSiteScanner(libraryCalls).scan(defined).isEmpty()) {
                log.fail(subject + className + " still calls the windowing library as the JVM defined it");
            }
            PinnedSites.foundAt(new CallSiteScanner(hookCalls).scan(defined))
                    .forEach((site, count) -> redirected.merge(site, count, Integer::sum));
        }
        return redirected;
    }

    private static Optional<List<Path>> librariesOf(LauncherInstall install, String subject, ScenarioLog log)
            throws IOException {
        List<Path> libraries = new ArrayList<>();
        for (Library library : install.profile().libraries()) {
            Optional<Path> local = localCopyOf(library);
            if (local.isEmpty()) {
                log.fail(subject + "library " + library.name() + " is not the one this tool runs with, so its pin"
                        + " cannot be checked offline");
                return Optional.empty();
            }
            libraries.add(local.get());
        }
        return Optional.of(libraries);
    }

    private static Optional<Path> localCopyOf(Library library) throws IOException {
        Path ownAsm;
        try {
            ownAsm = Path.of(ClassReader.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException malformed) {
            throw new IOException("the location of the ASM library is not a file", malformed);
        }
        boolean isSameArtifact = Files.size(ownAsm) == library.downloads().artifact().size()
                && PinnedDownloads.sha1Of(ownAsm).equals(library.downloads().artifact().sha1());
        return isSameArtifact ? Optional.of(ownAsm) : Optional.empty();
    }

    private static List<Path> classPathOf(Path client, List<Path> libraries) {
        return Stream.concat(Stream.of(client), libraries.stream()).toList();
    }

    private static Set<String> hookedClassesOf(ReleaseHooks hooks) {
        Set<String> classes = new LinkedHashSet<>();
        for (CallSiteRedirect redirect : ReleasePlans.planOf(hooks).redirects()) {
            classes.add(redirect.enclosingClass());
        }
        return classes;
    }

    private static ReleaseTarget releaseOf(TargetManifest targets, String id) {
        return targets.releases().stream()
                .filter(release -> release.id().equals(id))
                .findFirst()
                .orElseThrow();
    }

    private static String gradlePathOf(String module) {
        return ":" + module.replace('/', ':');
    }

    private record Client(ReleaseTarget release, Path jar) {}

    private static Path emptied(Path directory) throws IOException {
        if (Files.exists(directory)) {
            try (Stream<Path> contents = Files.walk(directory)) {
                for (Path path : contents.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(path);
                }
            }
        }
        return Files.createDirectories(directory);
    }
}
