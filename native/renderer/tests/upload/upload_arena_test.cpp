#include <algorithm>
#include <array>
#include <cstddef>
#include <vector>

#include <doctest.h>

#include "resources/buffer.hpp"
#include "support/validated_device.hpp"
#include "upload/upload_arena.hpp"

namespace {

using namespace minemod::renderer;
using namespace minemod::renderer::test_support;

constexpr std::array first_block{std::byte{0x11}, std::byte{0x22}, std::byte{0x33}, std::byte{0x44}};
constexpr std::array second_block{
    std::byte{0xa1}, std::byte{0xa2}, std::byte{0xa3}, std::byte{0xa4},
    std::byte{0xa5}, std::byte{0xa6}, std::byte{0xa7}, std::byte{0xa8},
};

}

TEST_CASE("staged bytes reach a device buffer through recorded copies") {
    const ValidatedDevice gpu;
    UploadArena arena(gpu.device(), 64);
    const Buffer destination(gpu.device(), 16, VK_BUFFER_USAGE_TRANSFER_DST_BIT, host_memory);
    const ArenaRegion first = arena.stage(first_block, 4).value();
    const ArenaRegion second = arena.stage(second_block, 8).value();

    gpu.device().submit([&](VkCommandBuffer commands) {
        const std::array copies{
            VkBufferCopy{first.offset, 0, first.size},
            VkBufferCopy{second.offset, 8, second.size},
        };
        vkCmdCopyBuffer(commands, arena.buffer(), destination.get(), 2, copies.data());
    });

    const std::vector<std::byte> stored = destination.read();
    CHECK(std::equal(first_block.begin(), first_block.end(), stored.begin()));
    CHECK(std::equal(second_block.begin(), second_block.end(), stored.begin() + 8));
    CHECK(gpu.validation_text() == "");
}

TEST_CASE("a full arena refuses an upload and accepts it after a reset") {
    const ValidatedDevice gpu;
    UploadArena arena(gpu.device(), 8);
    REQUIRE(arena.stage(second_block, 1).has_value());

    CHECK_FALSE(arena.stage(first_block, 1).has_value());
    arena.reset();

    CHECK(arena.stage(first_block, 1) == ArenaRegion{0, 4});
    CHECK(gpu.validation_text() == "");
}
