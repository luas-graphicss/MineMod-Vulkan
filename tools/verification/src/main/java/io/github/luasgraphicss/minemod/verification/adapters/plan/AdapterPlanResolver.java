package io.github.luasgraphicss.minemod.verification.adapters.plan;

import io.github.luasgraphicss.minemod.adapters.transformers.release.ReleasePlan;
import io.github.luasgraphicss.minemod.verification.adapters.names.NamespacePolicy;
import io.github.luasgraphicss.minemod.verification.adapters.names.RuntimeNamespace;
import io.github.luasgraphicss.minemod.verification.adapters.names.TinyNames;
import io.github.luasgraphicss.minemod.verification.adapters.names.TsrgNames;
import io.github.luasgraphicss.minemod.verification.adapters.source.NameDownloads;
import io.github.luasgraphicss.minemod.verification.adapters.source.NameSource;
import io.github.luasgraphicss.minemod.verification.adapters.source.NameSources;
import io.github.luasgraphicss.minemod.verification.hooks.cache.PinnedDownloads;
import io.github.luasgraphicss.minemod.verification.hooks.model.ReleaseHooks;
import io.github.luasgraphicss.minemod.verification.targets.model.LoaderTarget;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class AdapterPlanResolver {
    private static final int MAX_NAME_FILE_BYTES = 64 * 1024 * 1024;

    private final NameDownloads downloads;

    public AdapterPlanResolver(NameDownloads downloads) {
        this.downloads = downloads;
    }

    public AdapterPlan resolve(ReleaseHooks hooks, LoaderTarget loader) throws IOException, InterruptedException {
        RuntimeNamespace namespace = NamespacePolicy.of(loader);
        if (!namespace.isReadFromMappings()) {
            return AdapterPlan.written(RuntimePlans.written(hooks, namespace), namespace);
        }
        NameSource source = NameSources.of(hooks.release(), namespace);
        return mapped(hooks, namespace, source, downloads.fetch(source));
    }

    public AdapterPlan resolveAsPinned(ReleaseHooks hooks, LoaderTarget loader, AdapterPlan pinned)
            throws IOException, InterruptedException {
        RuntimeNamespace namespace = NamespacePolicy.of(loader);
        if (!namespace.isReadFromMappings()) {
            return AdapterPlan.written(RuntimePlans.written(hooks, namespace), namespace);
        }
        NameSource source = NameSources.of(hooks.release(), namespace);
        return mapped(hooks, namespace, source, downloads.fetchPinned(source, pinned.namesSha1()));
    }

    private static AdapterPlan mapped(ReleaseHooks hooks, RuntimeNamespace namespace, NameSource source, Path archive)
            throws IOException {
        ReleasePlan plan;
        try (BufferedReader names = readerOf(archive, source.entry())) {
            plan = namespace == RuntimeNamespace.INTERMEDIARY
                    ? RuntimePlans.intermediary(hooks, TinyNames.read(names))
                    : RuntimePlans.srg(hooks, TsrgNames.read(names));
        }
        return AdapterPlan.mapped(plan, namespace, source.url(), PinnedDownloads.sha1Of(archive));
    }

    private static BufferedReader readerOf(Path archive, String entryName) throws IOException {
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            ZipEntry entry = zip.getEntry(entryName);
            if (entry == null) {
                throw new IOException(archive.getFileName() + " has no " + entryName);
            }
            try (InputStream content = zip.getInputStream(entry)) {
                byte[] bytes = content.readNBytes(MAX_NAME_FILE_BYTES + 1);
                if (bytes.length > MAX_NAME_FILE_BYTES) {
                    throw new IOException(entryName + " exceeds " + MAX_NAME_FILE_BYTES + " bytes");
                }
                return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8));
            }
        }
    }
}
