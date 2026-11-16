package io.github.luasgraphicss.minemod.codec;

import io.github.luasgraphicss.minemod.contract.handle.HandleAllocator;
import io.github.luasgraphicss.minemod.contract.handle.ResourceHandle;
import io.github.luasgraphicss.minemod.contract.handle.ResourceKind;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

public final class CommandSequenceGenerator {
    public static final class Sequence {
        public final ByteBuffer stream;
        public final int commandCount;

        Sequence(ByteBuffer stream, int commandCount) {
            this.stream = stream;
            this.commandCount = commandCount;
        }
    }

    private static final int SLOT_COUNT = 16;
    private static final int MAX_COMMANDS = 12;
    private static final int MAX_BUFFER_BYTES = 64;
    private static final int STREAM_CAPACITY = 4096;
    private static final int[] INTERESTING_LENGTHS = {0, 1, 7, 8, 0x7FFFFFFF, 0xFFFFFFFF};

    private final Random random;
    private final HandleAllocator handles = new HandleAllocator(ResourceKind.BUFFER, SLOT_COUNT);
    private final List<ResourceHandle> liveBuffers = new ArrayList<>();
    private final List<Integer> liveSizes = new ArrayList<>();

    public CommandSequenceGenerator(Random random) {
        this.random = random;
    }

    public Sequence next() {
        CommandStreamWriter writer = new CommandStreamWriter(ByteBuffer.allocate(STREAM_CAPACITY));
        int commandCount = 1 + random.nextInt(MAX_COMMANDS);
        for (int command = 0; command < commandCount; command++) {
            appendCommand(writer);
        }
        return new Sequence(writer.finish(), commandCount);
    }

    public static ByteBuffer mutate(ByteBuffer stream, Random random) {
        byte[] bytes = new byte[stream.remaining()];
        stream.duplicate().get(bytes);
        ByteBuffer mutated = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        switch (random.nextInt(3)) {
            case 0:
                int position = random.nextInt(bytes.length);
                bytes[position] ^= (byte) (1 << random.nextInt(8));
                break;
            case 1:
                int field = random.nextInt(bytes.length / 4) * 4;
                mutated.putInt(field, INTERESTING_LENGTHS[random.nextInt(INTERESTING_LENGTHS.length)]);
                break;
            default:
                mutated.limit(random.nextInt(bytes.length + 1));
                break;
        }
        return mutated;
    }

    private void appendCommand(CommandStreamWriter writer) {
        boolean mustCreate = liveBuffers.isEmpty();
        boolean canCreate = liveBuffers.size() < SLOT_COUNT;
        int action = mustCreate ? 0 : random.nextInt(canCreate ? 3 : 2) + (canCreate ? 0 : 1);
        if (action == 0) {
            appendCreate(writer);
        } else if (action == 1) {
            appendWrite(writer);
        } else {
            appendDestroy(writer);
        }
    }

    private void appendCreate(CommandStreamWriter writer) {
        ResourceHandle buffer = handles.allocate();
        int sizeBytes = 1 + random.nextInt(MAX_BUFFER_BYTES);
        BufferUsage usage = BufferUsage.values()[random.nextInt(BufferUsage.values().length)];
        writer.createBuffer(buffer, sizeBytes, EnumSet.of(usage));
        liveBuffers.add(buffer);
        liveSizes.add(sizeBytes);
    }

    private void appendWrite(CommandStreamWriter writer) {
        int live = random.nextInt(liveBuffers.size());
        int sizeBytes = liveSizes.get(live);
        int offset = random.nextInt(sizeBytes);
        byte[] data = new byte[1 + random.nextInt(sizeBytes - offset)];
        random.nextBytes(data);
        writer.writeBuffer(liveBuffers.get(live), offset, ByteBuffer.wrap(data));
    }

    private void appendDestroy(CommandStreamWriter writer) {
        int live = random.nextInt(liveBuffers.size());
        ResourceHandle buffer = liveBuffers.remove(live);
        liveSizes.remove(live);
        handles.release(buffer);
        writer.destroyBuffer(buffer);
    }
}
