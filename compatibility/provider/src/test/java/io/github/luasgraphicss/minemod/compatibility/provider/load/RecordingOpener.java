package io.github.luasgraphicss.minemod.compatibility.provider.load;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class RecordingOpener implements LibraryOpener {
    static final long HANDLE = 0x7F00L;

    private final List<Path> opened = new ArrayList<>();
    private final List<Long> closed = new ArrayList<>();
    private boolean refusing;

    @Override
    public long open(Path library) {
        if (refusing) {
            return FAILED;
        }
        opened.add(library);
        return HANDLE;
    }

    @Override
    public void close(long handle) {
        closed.add(handle);
    }

    void refuse() {
        refusing = true;
    }

    List<Path> opened() {
        return opened;
    }

    List<Long> closed() {
        return closed;
    }
}
