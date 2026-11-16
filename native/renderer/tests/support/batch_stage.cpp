#include "support/batch_stage.hpp"

#include <cstddef>
#include <cstdint>
#include <optional>
#include <utility>

#include "environment/fog.hpp"
#include "frame/frame_rendering.hpp"
#include "graph/graph_images.hpp"
#include "graph/image_use.hpp"
#include "graph/planned_frame.hpp"
#include "graph/render_graph.hpp"
#include "math/matrix.hpp"
#include "math/vector.hpp"
#include "textures/lightmap.hpp"
#include "textures/mip_chain.hpp"

namespace minemod::renderer::test_support {
namespace {

constexpr VkExtent2D frame_extent{64, 64};
constexpr VkExtent2D surface_extent{2, 1};
constexpr VkExtent2D overlay_extent{16, 16};
constexpr VkDeviceSize upload_capacity = 4096;
constexpr VkDeviceSize geometry_capacity = 8192;

constexpr std::array<float, 4> untinted{1.0f, 1.0f, 1.0f, 1.0f};
constexpr std::array<float, 4> black{0.0f, 0.0f, 0.0f, 1.0f};
constexpr Fog clear_air{black, 1000.0f, 2000.0f};
constexpr Vector3 light_from_above{0.0f, 1.0f, 0.0f};
constexpr Vector3 no_second_light{0.0f, 0.0f, 0.0f};

constexpr std::array<std::uint8_t, 8> red_then_green{255, 0, 0, 255, 0, 255, 0, 255};
constexpr std::array<std::uint8_t, 4> opaque_white{255, 255, 255, 255};
constexpr std::array<std::uint8_t, 4> untouched_by_overlay{0, 0, 0, 255};

constexpr SamplerDescription nearest_sampler{
    VK_FILTER_NEAREST,
    VK_FILTER_NEAREST,
    VK_SAMPLER_MIPMAP_MODE_NEAREST,
    VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE,
    1,
};

std::vector<std::byte> filled(VkExtent2D extent, const std::array<std::uint8_t, 4>& texel) {
    const std::span<const std::byte> channels = std::as_bytes(std::span(texel));
    std::vector<std::byte> rgba;
    rgba.reserve(std::size_t{extent.width} * extent.height * channels.size());
    for (std::uint32_t filled_texels = 0; filled_texels < extent.width * extent.height; ++filled_texels) {
        rgba.insert(rgba.end(), channels.begin(), channels.end());
    }
    return rgba;
}

ImportedImage imported(
    const Image& image, VkImageAspectFlags aspect, const ImageUse& entry, const std::optional<ImageUse>& exit) {
    return ImportedImage{image.get(), image.view(), image.extent(), aspect, entry, exit};
}

}

BatchStage::BatchStage()
    : targets_(gpu_.device(), frame_extent),
      uploads_(gpu_.device(), upload_capacity),
      geometry_(gpu_.device(), geometry_capacity, geometry_arena_usage),
      stager_(geometry_),
      surface_(gpu_.device(), surface_extent, 1),
      lightmap_(gpu_.device(), lightmap_extent, 1),
      overlay_(gpu_.device(), overlay_extent, 1),
      samplers_(gpu_.device()),
      pipelines_(gpu_.device()),
      descriptors_(
          gpu_.device(),
          DescriptorPoolShape{
              2,
              {{VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, 1}, {VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, 3}},
              1,
          }),
      view_constants_(gpu_.device()),
      renderer_(gpu_.device(), pipelines_) {
    upload(surface_, surface_extent, std::as_bytes(std::span(red_then_green)));
    upload(lightmap_, lightmap_extent, filled(lightmap_extent, opaque_white));
    upload(overlay_, overlay_extent, filled(overlay_extent, untouched_by_overlay));
    view_constants_.write(SceneView{identity_matrix, clear_air, {light_from_above, no_second_light}});

    const VkSampler sampler = samplers_.sampler_for(nearest_sampler);
    view_ = renderer_.bind_view(
        descriptors_, view_constants_, ViewTextures{lightmap_.image().view(), sampler, overlay_.image().view(), sampler});
    surface_set_ = renderer_.bind_texture(descriptors_, surface_.image().view(), sampler);
}

DrawBatch BatchStage::quads(
    const VertexFormat& format, std::span<const SceneVertex> corners, const Material& material, QuadOrder order) {
    const std::vector<std::byte> bytes = encode_vertices(format, corners);
    const BatchGeometry staged = stager_.stage({format, PrimitiveMode::quads, bytes}, order).value();
    return DrawBatch{format, staged, material, surface_set_, identity_matrix, untinted};
}

void BatchStage::draw(const DrawBatch& batch) {
    batches_.push_back(batch);
}

FramePixels BatchStage::render() {
    RenderGraph graph;
    const ImageId color = graph.import_image(
        "frame color", imported(targets_.color(), VK_IMAGE_ASPECT_COLOR_BIT, uses::untouched, uses::transfer_source));
    const ImageId depth = graph.import_image(
        "frame depth", imported(targets_.depth(), VK_IMAGE_ASPECT_DEPTH_BIT, uses::untouched, std::nullopt));
    graph.add_pass(
        "batches",
        {{color, uses::color_attachment}, {depth, uses::depth_attachment}},
        [this, color, depth](VkCommandBuffer commands, const GraphImages& images) {
            begin_frame_rendering(commands, {images.at(color), images.at(depth)}, black);
            renderer_.record(commands, {frame_extent, view_}, batches_);
            vkCmdEndRendering(commands);
        });

    const PlannedFrame planned(gpu_.device(), std::move(graph));
    gpu_.device().submit([&planned](VkCommandBuffer commands) { planned.record(commands); });
    return read_back_color(gpu_.device(), targets_.color());
}

void BatchStage::upload(const Texture& texture, VkExtent2D extent, std::span<const std::byte> rgba) {
    const MipChain chain = build_mip_chain(extent, rgba, 1);
    uploads_.reset();
    const ArenaRegion staged = uploads_.stage(chain.rgba, 4).value();
    gpu_.device().submit([&](VkCommandBuffer commands) {
        texture.record_upload(commands, uploads_.buffer(), staged, chain);
    });
}

}
