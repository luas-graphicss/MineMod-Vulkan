package io.github.luasgraphicss.minemod.journey.plan;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public record JourneyPlanFile(JourneyPlan plan, String sha256) {
    public static JourneyPlanFile read(Path file) {
        try {
            byte[] contents = Files.readAllBytes(file);
            JourneyPlan plan = new Gson().fromJson(new String(contents, StandardCharsets.UTF_8), JourneyPlan.class);
            return new JourneyPlanFile(plan, HexFormat.of().formatHex(sha256Of(contents)));
        } catch (IOException unreadable) {
            throw new UncheckedIOException("the journey plan " + file + " cannot be read", unreadable);
        }
    }

    private static byte[] sha256Of(byte[] contents) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(contents);
        } catch (NoSuchAlgorithmException missing) {
            throw new IllegalStateException("every Java runtime provides SHA-256", missing);
        }
    }
}
