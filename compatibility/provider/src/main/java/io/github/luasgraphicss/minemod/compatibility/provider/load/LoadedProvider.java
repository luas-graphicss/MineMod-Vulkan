package io.github.luasgraphicss.minemod.compatibility.provider.load;

public final class LoadedProvider implements AutoCloseable {
    private final long handle;
    private final String libraryFileName;
    private final LibraryOpener opener;
    private boolean closed;

    LoadedProvider(long handle, String libraryFileName, LibraryOpener opener) {
        this.handle = handle;
        this.libraryFileName = libraryFileName;
        this.opener = opener;
    }

    public long handle() {
        if (closed) {
            throw new IllegalStateException(libraryFileName + " was already unloaded");
        }
        return handle;
    }

    public String libraryFileName() {
        return libraryFileName;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        opener.close(handle);
    }
}
