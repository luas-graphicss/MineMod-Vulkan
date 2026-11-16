#include <algorithm>
#include <cstddef>
#include <cstdint>
#include <optional>
#include <stdexcept>
#include <utility>
#include <vector>

#include <doctest.h>

#include "graph/image_use.hpp"
#include "graph/planned_frame.hpp"
#include "graph/render_graph.hpp"
#include "resources/buffer.hpp"
#include "resources/image_layout.hpp"
#include "support/validated_device.hpp"
#include "textures/lightmap.hpp"
#include "textures/mip_chain.hpp"
#include "textures/texture.hpp"
#include "upload/upload_arena.hpp"

namespace {

using namespace minemod::renderer;
using namespace minemod::renderer::test_support;

constexpr VkExtent2D texture_extent{4, 4};
constexpr std::uint32_t texture_levels = 3;

constexpr LayoutChange sampled_to_transfer_source{
    VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
    VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
    VK_PIPELINE_STAGE_2_FRAGMENT_SHADER_BIT,
    VK_ACCESS_2_SHADER_READ_BIT,
    VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT,
    VK_ACCESS_2_TRANSFER_READ_BIT,
};

constexpr LayoutChange transfer_source_to_sampled{
    VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
    VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
    VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT,
    VK_ACCESS_2_TRANSFER_READ_BIT,
    VK_PIPELINE_STAGE_2_FRAGMENT_SHADER_BIT,
    VK_ACCESS_2_SHADER_READ_BIT,
};

std::vector<std::byte> gradient_texels(VkExtent2D extent) {
    std::vector<std::byte> texels;
    for (std::uint32_t texel = 0; texel < extent.width * extent.height; ++texel) {
        const auto shade = static_cast<std::uint8_t>(texel * 16 + 15);
        const auto inverse_shade = static_cast<std::uint8_t>(255 - shade);
        texels.insert(texels.end(), {std::byte{shade}, std::byte{inverse_shade}, std::byte{0x40}, std::byte{0xff}});
    }
    return texels;
}

std::vector<std::byte> read_level(
    const Device& device, const Texture& texture, const MipLevel& mip, std::uint32_t level) {
    const VkDeviceSize byte_count = VkDeviceSize{mip.extent.width} * mip.extent.height * 4;
    const Buffer readback(device, byte_count, VK_BUFFER_USAGE_TRANSFER_DST_BIT, host_memory);
    const VkImage image = texture.image().get();
    device.submit([&](VkCommandBuffer commands) {
        const VkBufferImageCopy region{
            .imageSubresource = {VK_IMAGE_ASPECT_COLOR_BIT, level, 0, 1},
            .imageExtent = {mip.extent.width, mip.extent.height, 1},
        };
        change_layout(commands, image, VK_IMAGE_ASPECT_COLOR_BIT, sampled_to_transfer_source);
        vkCmdCopyImageToBuffer(commands, image, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, readback.get(), 1, &region);
        change_layout(commands, image, VK_IMAGE_ASPECT_COLOR_BIT, transfer_source_to_sampled);
    });
    return readback.read();
}

}

TEST_CASE("every level of a staged mip chain reaches the texture") {
    const ValidatedDevice gpu;
    const MipChain chain = build_mip_chain(texture_extent, gradient_texels(texture_extent), texture_levels);
    const Texture texture(gpu.device(), texture_extent, texture_levels);
    UploadArena arena(gpu.device(), 256);
    const ArenaRegion staged = arena.stage(chain.rgba, 4).value();

    gpu.device().submit([&](VkCommandBuffer commands) {
        texture.record_upload(commands, arena.buffer(), staged, chain);
    });

    for (std::uint32_t level = 0; level < texture_levels; ++level) {
        const std::vector<std::byte> stored = read_level(gpu.device(), texture, chain.levels[level], level);
        CHECK(std::ranges::equal(stored, chain.level_bytes(level)));
    }
    CHECK(gpu.validation_text() == "");
}

TEST_CASE("updating a region keeps every texel outside it") {
    constexpr SpriteRegion center{1, 1, 2, 2};
    constexpr std::byte updated_shade{0x7f};
    const ValidatedDevice gpu;
    const std::vector<std::byte> original = gradient_texels(texture_extent);
    const MipChain chain = build_mip_chain(texture_extent, original, 1);
    const Texture texture(gpu.device(), texture_extent, 1);
    UploadArena arena(gpu.device(), 256);
    const ArenaRegion uploaded = arena.stage(chain.rgba, 4).value();
    gpu.device().submit([&](VkCommandBuffer commands) {
        texture.record_upload(commands, arena.buffer(), uploaded, chain);
    });
    const std::vector<std::byte> replacement(std::size_t{center.width} * center.height * 4, updated_shade);
    const ArenaRegion staged = arena.stage(replacement, 4).value();

    RenderGraph graph;
    const Image& image = texture.image();
    const ImageId target = graph.import_image(
        "dynamic texture",
        ImportedImage{
            image.get(),
            image.view(),
            image.extent(),
            VK_IMAGE_ASPECT_COLOR_BIT,
            uses::sampled_by_shaders,
            uses::sampled_by_shaders,
        });
    graph.add_pass(
        "update region",
        {{target, uses::transfer_destination}},
        [&](VkCommandBuffer commands, const GraphImages&) {
            texture.record_region_update(commands, arena.buffer(), staged, 0, center);
        });
    const PlannedFrame planned(gpu.device(), std::move(graph));
    gpu.device().submit([&planned](VkCommandBuffer commands) { planned.record(commands); });

    const std::vector<std::byte> stored = read_level(gpu.device(), texture, chain.levels[0], 0);
    for (std::uint32_t y = 0; y < texture_extent.height; ++y) {
        for (std::uint32_t x = 0; x < texture_extent.width; ++x) {
            const bool is_updated = x >= center.x && x < center.x + center.width && y >= center.y
                && y < center.y + center.height;
            const std::size_t texel = (std::size_t{y} * texture_extent.width + x) * 4;
            CHECK(stored[texel] == (is_updated ? updated_shade : original[texel]));
        }
    }
    CHECK(gpu.validation_text() == "");
}

TEST_CASE("a region update outside the texture level or with the wrong texel count is rejected") {
    const ValidatedDevice gpu;
    const Texture texture(gpu.device(), texture_extent, texture_levels);
    const ArenaRegion four_texels{0, 16};

    CHECK_THROWS_AS(
        texture.record_region_update(VK_NULL_HANDLE, VK_NULL_HANDLE, four_texels, 0, {3, 3, 2, 2}),
        std::invalid_argument);
    CHECK_THROWS_AS(
        texture.record_region_update(VK_NULL_HANDLE, VK_NULL_HANDLE, four_texels, 1, {1, 1, 2, 2}),
        std::invalid_argument);
    CHECK_THROWS_AS(
        texture.record_region_update(VK_NULL_HANDLE, VK_NULL_HANDLE, four_texels, texture_levels, {0, 0, 1, 1}),
        std::invalid_argument);
    CHECK_THROWS_AS(
        texture.record_region_update(VK_NULL_HANDLE, VK_NULL_HANDLE, four_texels, 0, {0, 0, 3, 1}),
        std::invalid_argument);
}

TEST_CASE("a mip chain that does not match the texture is rejected before anything is recorded") {
    const ValidatedDevice gpu;
    const Texture lightmap(gpu.device(), lightmap_extent, 1);
    const MipChain chain = build_mip_chain(texture_extent, gradient_texels(texture_extent), 1);
    const ArenaRegion staged{0, chain.rgba.size()};

    CHECK_THROWS_AS(lightmap.record_upload(VK_NULL_HANDLE, VK_NULL_HANDLE, staged, chain), std::invalid_argument);
}
