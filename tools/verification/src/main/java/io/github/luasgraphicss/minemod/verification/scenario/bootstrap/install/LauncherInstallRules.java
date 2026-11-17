package io.github.luasgraphicss.minemod.verification.scenario.bootstrap.install;

import io.github.luasgraphicss.minemod.verification.scenario.bootstrap.install.LauncherProfile.Artifact;
import io.github.luasgraphicss.minemod.verification.scenario.bootstrap.install.LauncherProfile.Library;
import io.github.luasgraphicss.minemod.verification.targets.model.ReleaseTarget;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class LauncherInstallRules {
    private static final Pattern SHA1 = Pattern.compile("[0-9a-f]{40}");
    private static final String SECURE_SCHEME = "https://";
    private static final String CLASS_SUFFIX = ".class";
    private static final String OWN_CLASSES = "io/github/luasgraphicss/minemod/";

    private LauncherInstallRules() {}

    public static List<String> findViolations(LauncherInstall install, ReleaseTarget release) throws IOException {
        List<String> violations = new ArrayList<>();
        LauncherProfile profile = install.profile();
        if (!release.id().equals(profile.inheritsFrom())) {
            violations.add("the profile inherits " + profile.inheritsFrom() + ", expected " + release.id());
        }
        if (!release.launch().mainClass().equals(profile.mainClass())) {
            violations.add("the profile starts " + profile.mainClass() + ", the game starts "
                    + release.launch().mainClass());
        }
        if (!profile.arguments().game().isEmpty()) {
            violations.add("the profile adds game arguments: " + profile.arguments().game());
        }
        checkAgent(install, violations);
        for (Library library : profile.libraries()) {
            checkLibrary(library, violations);
        }
        return violations;
    }

    private static void checkAgent(LauncherInstall install, List<String> violations) throws IOException {
        List<String> declared = install.profile().arguments().jvm();
        String ownLibrary = LauncherInstall.AGENT_OPTION + LauncherInstall.LIBRARY_DIRECTORY + "/";
        if (declared.size() != 1 || !declared.getFirst().startsWith(ownLibrary)) {
            violations.add("the profile adds " + declared + ", expected one agent under " + ownLibrary);
            return;
        }
        Path agent = install.agents().getFirst();
        if (!Files.isRegularFile(agent)) {
            violations.add("the install has no " + install.directory().relativize(agent));
            return;
        }
        List<Path> expected = Stream.of(agent, install.profileFile()).sorted().toList();
        if (!install.files().equals(expected)) {
            violations.add("the install holds more than the profile and the agent: " + install.files());
        }
        try (ZipFile jar = new ZipFile(agent.toFile())) {
            List<String> foreignClasses = jar.stream()
                    .map(ZipEntry::getName)
                    .filter(name -> name.endsWith(CLASS_SUFFIX) && !name.startsWith(OWN_CLASSES))
                    .toList();
            if (!foreignClasses.isEmpty()) {
                violations.add("the agent carries classes that are not its own: " + foreignClasses);
            }
        }
    }

    private static void checkLibrary(Library library, List<String> violations) {
        Artifact artifact = library.downloads().artifact();
        if (!artifact.url().startsWith(SECURE_SCHEME) || !artifact.url().endsWith(artifact.path())) {
            violations.add("library " + library.name() + " is not downloaded over HTTPS to its path: " + artifact.url());
        }
        if (!SHA1.matcher(artifact.sha1()).matches() || artifact.size() <= 0) {
            violations.add("library " + library.name() + " has no SHA-1 and size pin");
        }
    }
}
