package io.github.luasgraphicss.minemod.verification.compatibility.evidence;

import io.github.luasgraphicss.minemod.verification.compatibility.model.CompatibilityCase;
import io.github.luasgraphicss.minemod.verification.compatibility.model.CompatibilityCase.Evidence;
import io.github.luasgraphicss.minemod.verification.compatibility.model.CompatibilityManifest;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

public final class EvidenceFiles {
    private static final int BUFFER_BYTES = 64 * 1024;

    private EvidenceFiles() {}

    public static List<String> findViolations(Path repositoryRoot, CompatibilityManifest manifest)
            throws IOException {
        Path root = repositoryRoot.toAbsolutePath().normalize();
        List<String> violations = new ArrayList<>();
        for (CompatibilityCase entry : manifest.cases()) {
            for (Evidence evidence : entry.evidence()) {
                String subject = "case " + entry.id() + ": evidence " + evidence.path() + " ";
                Path file = root.resolve(evidence.path()).normalize();
                if (!file.startsWith(root)) {
                    violations.add(subject + "is outside the repository");
                } else if (!Files.isRegularFile(file)) {
                    violations.add(subject + "does not exist");
                } else if (!sha256Of(file).equals(evidence.sha256())) {
                    violations.add(subject + "does not match its SHA-256 pin");
                }
            }
        }
        return violations;
    }

    private static String sha256Of(Path file) throws IOException {
        MessageDigest digest = sha256();
        byte[] buffer = new byte[BUFFER_BYTES];
        try (InputStream content = Files.newInputStream(file)) {
            for (int read = content.read(buffer); read != -1; read = content.read(buffer)) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("every Java runtime provides SHA-256", exception);
        }
    }
}
