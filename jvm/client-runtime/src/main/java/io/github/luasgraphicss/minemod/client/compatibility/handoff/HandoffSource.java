package io.github.luasgraphicss.minemod.client.compatibility.handoff;

import io.github.luasgraphicss.minemod.client.compatibility.sharing.ImportedAttachments;
import io.github.luasgraphicss.minemod.compatibility.state.submission.AttachmentHandoff;

interface HandoffSource extends AutoCloseable {
    AttachmentHandoff over(ImportedAttachments imported);

    @Override
    default void close() {}
}
