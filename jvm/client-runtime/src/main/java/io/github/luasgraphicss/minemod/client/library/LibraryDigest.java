package io.github.luasgraphicss.minemod.client.library;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

final class LibraryDigest {
    private static final String ALGORITHM = "SHA-256";
    private static final int HEX_DIGITS = 64;
    private static final int HEX_RADIX = 16;
    private static final int BLOCK_BYTES = 64 * 1024;

    private LibraryDigest() {}

    static boolean isWellFormed(String digest) {
        if (digest.length() != HEX_DIGITS) {
            return false;
        }
        for (int index = 0; index < HEX_DIGITS; index++) {
            char digit = digest.charAt(index);
            boolean isLowercaseHex = (digit >= '0' && digit <= '9') || (digit >= 'a' && digit <= 'f');
            if (!isLowercaseHex) {
                return false;
            }
        }
        return true;
    }

    static String of(Path file) throws IOException {
        MessageDigest digest = newDigest();
        try (InputStream content = Files.newInputStream(file)) {
            byte[] block = new byte[BLOCK_BYTES];
            for (int read = content.read(block); read >= 0; read = content.read(block)) {
                digest.update(block, 0, read);
            }
        }
        return hex(digest.digest());
    }

    static String copy(InputStream source, Path destination) throws IOException {
        MessageDigest digest = newDigest();
        try (OutputStream target = Files.newOutputStream(destination)) {
            byte[] block = new byte[BLOCK_BYTES];
            for (int read = source.read(block); read >= 0; read = source.read(block)) {
                digest.update(block, 0, read);
                target.write(block, 0, read);
            }
        }
        return hex(digest.digest());
    }

    private static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance(ALGORITHM);
        } catch (NoSuchAlgorithmException missing) {
            throw new IllegalStateException("every Java runtime provides " + ALGORITHM, missing);
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder text = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            text.append(Character.forDigit((value >> 4) & 0xF, HEX_RADIX));
            text.append(Character.forDigit(value & 0xF, HEX_RADIX));
        }
        return text.toString();
    }
}
