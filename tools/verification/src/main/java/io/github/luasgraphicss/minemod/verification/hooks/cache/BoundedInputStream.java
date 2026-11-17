package io.github.luasgraphicss.minemod.verification.hooks.cache;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

final class BoundedInputStream extends FilterInputStream {
    private final long limit;
    private long consumed;

    BoundedInputStream(InputStream content, long limit) {
        super(content);
        this.limit = limit;
    }

    @Override
    public int read() throws IOException {
        int value = super.read();
        if (value != -1) {
            count(1);
        }
        return value;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        int read = super.read(buffer, offset, length);
        if (read > 0) {
            count(read);
        }
        return read;
    }

    private void count(int read) throws IOException {
        consumed += read;
        if (consumed > limit) {
            throw new IOException("the download exceeds its pinned size of " + limit + " bytes");
        }
    }
}
