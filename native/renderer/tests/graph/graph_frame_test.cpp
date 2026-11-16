#include <cstdint>
#include <utility>

#include <doctest.h>

#include "frame/frame_readback.hpp"
#include "graph/planned_frame.hpp"
#include "graph/render_graph.hpp"
#include "resources/image.hpp"
#include "support/graph_passes.hpp"
#include "support/graph_targets.hpp"
#include "support/validated_device.hpp"

namespace {

using namespace minemod::renderer;
using namespace minemod::renderer::test_support;

constexpr Pixel red{255, 0, 0, 255};
constexpr Pixel blue{0, 0, 255, 255};
constexpr VkExtent2D two_targets_wide{target_extent.width * 2, target_extent.height};

Image create_output(const Device& device, VkExtent2D extent) {
    return Image(
        device,
        ImageDescription{
            extent,
            VK_FORMAT_R8G8B8A8_UNORM,
            VK_IMAGE_USAGE_SAMPLED_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_TRANSFER_SRC_BIT,
            VK_IMAGE_ASPECT_COLOR_BIT,
        });
}

ImportedImage read_back_after(const Image& output) {
    return ImportedImage{
        output.get(),
        output.view(),
        output.extent(),
        VK_IMAGE_ASPECT_COLOR_BIT,
        uses::untouched,
        uses::transfer_source,
    };
}

void submit(const Device& device, const PlannedFrame& frame) {
    device.submit([&](VkCommandBuffer commands) { frame.record(commands); });
}

}

TEST_CASE("a planned frame clears its transient targets and delivers the color to an imported image") {
    const ValidatedDevice gpu;
    const Image output = create_output(gpu.device(), target_extent);
    RenderGraph graph;
    const ImageId delivered = graph.import_image("output", read_back_after(output));
    const ImageId scene_color = graph.create_image("scene color", color_target);
    const ImageId scene_depth = graph.create_image("scene depth", depth_target);
    graph.add_pass(
        "clear scene",
        {{scene_color, uses::color_attachment}, {scene_depth, uses::depth_attachment}},
        clear_color_and_depth(scene_color, scene_depth, red));
    graph.add_pass(
        "deliver scene",
        {{scene_color, uses::transfer_source}, {delivered, uses::transfer_destination}},
        copy_color(scene_color, delivered, {0, 0}));
    const PlannedFrame frame(gpu.device(), std::move(graph));

    submit(gpu.device(), frame);

    const FramePixels pixels = read_back_color(gpu.device(), output);
    CHECK(pixels.at(0, 0) == red);
    CHECK(pixels.at(target_extent.width - 1, target_extent.height - 1) == red);
    CHECK(gpu.validation_text() == "");
}

TEST_CASE("two targets that share one image deliver their own colors without a hazard") {
    const ValidatedDevice gpu;
    const Image output = create_output(gpu.device(), two_targets_wide);
    RenderGraph graph;
    const ImageId delivered = graph.import_image("output", read_back_after(output));
    const ImageId world = graph.create_image("world", color_target);
    const ImageId overlay = graph.create_image("overlay", color_target);
    graph.add_pass("clear world", {{world, uses::color_attachment}}, clear_color(world, red));
    graph.add_pass(
        "deliver world",
        {{world, uses::transfer_source}, {delivered, uses::transfer_destination}},
        copy_color(world, delivered, {0, 0}));
    graph.add_pass("clear overlay", {{overlay, uses::color_attachment}}, clear_color(overlay, blue));
    graph.add_pass(
        "deliver overlay",
        {{overlay, uses::transfer_source}, {delivered, uses::transfer_destination}},
        copy_color(overlay, delivered, {static_cast<std::int32_t>(target_extent.width), 0}));
    const PlannedFrame frame(gpu.device(), std::move(graph));

    submit(gpu.device(), frame);

    const FramePixels pixels = read_back_color(gpu.device(), output);
    CHECK(frame.plan().transients.descriptions.size() == 1);
    CHECK(pixels.at(0, 0) == red);
    CHECK(pixels.at(two_targets_wide.width - 1, two_targets_wide.height - 1) == blue);
    CHECK(gpu.validation_text() == "");
}

TEST_CASE("a planned frame records again without being planned again") {
    const ValidatedDevice gpu;
    const Image output = create_output(gpu.device(), target_extent);
    RenderGraph graph;
    const ImageId delivered = graph.import_image("output", read_back_after(output));
    const ImageId scene_color = graph.create_image("scene color", color_target);
    graph.add_pass("clear scene", {{scene_color, uses::color_attachment}}, clear_color(scene_color, blue));
    graph.add_pass(
        "deliver scene",
        {{scene_color, uses::transfer_source}, {delivered, uses::transfer_destination}},
        copy_color(scene_color, delivered, {0, 0}));
    const PlannedFrame frame(gpu.device(), std::move(graph));

    submit(gpu.device(), frame);
    submit(gpu.device(), frame);

    CHECK(read_back_color(gpu.device(), output).at(0, 0) == blue);
    CHECK(gpu.validation_text() == "");
}
