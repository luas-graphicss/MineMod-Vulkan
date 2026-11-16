package io.github.luasgraphicss.minemod.compatibility.layer.route;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import io.github.luasgraphicss.minemod.compatibility.layer.memory.NativeInts;
import io.github.luasgraphicss.minemod.compatibility.layer.memory.NativeMemory;
import io.github.luasgraphicss.minemod.compatibility.translation.framebuffer.VirtualFramebufferCalls;
import io.github.luasgraphicss.minemod.compatibility.translation.names.ObjectNames;
import io.github.luasgraphicss.minemod.compatibility.translation.provider.ProviderCalls;

final class FramebufferRoutes {
    private static final int GL_FRAMEBUFFER_DEFAULT = 0x8218;
    private static final int GL_TEXTURE = 0x1702;
    private static final int GL_RENDERBUFFER = 0x8D41;
    private static final int GL_ATTACHMENT_OBJECT_TYPE = 0x8CD0;
    private static final int GL_ATTACHMENT_OBJECT_NAME = 0x8CD1;
    private static final int NO_OBJECT = 0;
    private static final String ATTACHMENT_PARAMETER = "glGetFramebufferAttachmentParameteriv";
    private final VirtualFramebufferCalls framebuffers;
    private final ObjectNames names;
    private final ProviderCalls provider;
    private final NativeMemory memory;

    FramebufferRoutes(
            VirtualFramebufferCalls framebuffers, ObjectNames names, ProviderCalls provider, NativeMemory memory) {
        this.framebuffers = framebuffers;
        this.names = names;
        this.provider = provider;
        this.memory = memory;
    }

    void call(String coreFunction, String function, long[] words) {
        switch (coreFunction) {
            case "glBindFramebuffer":
                framebuffers.bind((int) words[0], (int) words[1]);
                break;
            case "glDeleteFramebuffers":
                framebuffers.delete(NativeInts.read(memory, words[1], (int) words[0]));
                break;
            case "glDrawBuffer":
                framebuffers.drawBuffer((int) words[0]);
                break;
            case "glReadBuffer":
                framebuffers.readBuffer((int) words[0]);
                break;
            case "glDrawBuffers":
                if (!framebuffers.drawBuffersOnDefault(NativeInts.read(memory, words[1], (int) words[0]))) {
                    provider.invoke(function, words);
                }
                break;
            case ATTACHMENT_PARAMETER:
                attachmentParameter(function, words);
                break;
            default:
                framebuffers.attach(function, words);
        }
    }

    private void attachmentParameter(String function, long[] words) {
        int target = (int) words[0];
        int attachment = (int) words[1];
        int parameter = (int) words[2];
        long answer = words[3];
        int providerAttachment = framebuffers.providerAttachment(target, attachment);
        if (providerAttachment == VirtualFramebufferCalls.REJECTED) {
            return;
        }
        boolean onDefaultFramebuffer = providerAttachment != attachment;
        if (onDefaultFramebuffer && parameter == GL_ATTACHMENT_OBJECT_TYPE) {
            memory.writeInt(answer, GL_FRAMEBUFFER_DEFAULT);
            return;
        }
        if (parameter == GL_ATTACHMENT_OBJECT_NAME) {
            memory.writeInt(answer, onDefaultFramebuffer ? NO_OBJECT : attachedName(target, providerAttachment, answer));
            return;
        }
        provider.invoke(function, new long[] {target, providerAttachment, parameter, answer});
    }

    private int attachedName(int target, int attachment, long scratch) {
        provider.invoke(ATTACHMENT_PARAMETER, new long[] {target, attachment, GL_ATTACHMENT_OBJECT_TYPE, scratch});
        int type = memory.readInt(scratch);
        provider.invoke(ATTACHMENT_PARAMETER, new long[] {target, attachment, GL_ATTACHMENT_OBJECT_NAME, scratch});
        int backing = memory.readInt(scratch);
        if (type == GL_TEXTURE) {
            return names.nameOfBacking(ObjectKind.TEXTURE, backing);
        }
        return type == GL_RENDERBUFFER ? names.nameOfBacking(ObjectKind.RENDERBUFFER, backing) : NO_OBJECT;
    }
}
