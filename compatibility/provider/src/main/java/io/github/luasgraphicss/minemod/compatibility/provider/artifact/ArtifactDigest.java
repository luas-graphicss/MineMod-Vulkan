package io.github.luasgraphicss.minemod.compatibility.provider.artifact;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class ArtifactDigest {
    private static final String ALGORITHM = "SHA-256";
    private static final int HEX_LENGTH = 64;
    private static final int READ_BUFFER_BYTES = 64 * 1024;
    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    private final String hex;

    private ArtifactDigest(String hex) {
        this.hex = hex;
    }

    public static ArtifactDigest ofHex(String hex) {
        if (hex.length() != HEX_LENGTH || !isLowercaseHex(hex)) {
            throw new IllegalArgumentException("a SHA-256 digest is 64 lowercase hexadecimal digits: " + hex);
        }
        return new ArtifactDigest(hex);
    }

    public static ArtifactDigest ofFile(Path file) throws IOException {
        MessageDigest digest = newDigest();
        byte[] buffer = new byte[READ_BUFFER_BYTES];
        try (InputStream input = Files.newInputStream(file)) {
            int read = input.read(buffer);
            while (read >= 0) {
                digest.update(buffer, 0, read);
                read = input.read(buffer);
            }
        }
        return new ArtifactDigest(toHex(digest.digest()));
    }

    public String hex() {
        return hex;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ArtifactDigest && hex.equals(((ArtifactDigest) other).hex);
    }

    @Override
    public int hashCode() {
        return hex.hashCode();
    }

    @Override
    public String toString() {
        return "sha256:" + hex;
    }

    private static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance(ALGORITHM);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(ALGORITHM + " is required of every Java runtime", exception);
        }
    }

    private static boolean isLowercaseHex(String text) {
        for (int index = 0; index < text.length(); index++) {
            char digit = text.charAt(index);
            if (!((digit >= '0' && digit <= '9') || (digit >= 'a' && digit <= 'f'))) {
                return false;
            }
        }
        return true;
    }

    private static String toHex(byte[] bytes) {
        char[] digits = new char[bytes.length * 2];
        for (int index = 0; index < bytes.length; index++) {
            digits[index * 2] = HEX_DIGITS[(bytes[index] >> 4) & 0xF];
            digits[index * 2 + 1] = HEX_DIGITS[bytes[index] & 0xF];
        }
        return new String(digits);
    }
}
