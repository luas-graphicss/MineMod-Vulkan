#include <doctest.h>

#include "graph/graph_plan.hpp"
#include "graph/render_graph.hpp"
#include "support/graph_targets.hpp"

namespace {

using namespace minemod::renderer;
using namespace minemod::renderer::test_support;

ImportedImage imported_output() {
    return ImportedImage{
        VK_NULL_HANDLE,
        VK_NULL_HANDLE,
        target_extent,
        VK_IMAGE_ASPECT_COLOR_BIT,
        uses::untouched,
        uses::transfer_source,
    };
}

}

TEST_CASE("images used by the same pass keep separate slots") {
    RenderGraph graph;
    const ImageId scene = graph.create_image("scene", color_target);
    const ImageId blurred = graph.create_image("blurred", color_target);
    graph.add_pass("draw", {{scene, uses::color_attachment}}, record_nothing);
    graph.add_pass(
        "blur", {{scene, uses::sampled_by_fragment_shader}, {blurred, uses::color_attachment}}, record_nothing);

    const GraphPlan plan = plan_graph(graph);

    CHECK(plan.transients.descriptions.size() == 2);
    CHECK(plan.transients.slot_of_image.at(scene.index) != plan.transients.slot_of_image.at(blurred.index));
}

TEST_CASE("an image takes over the slot of an identical image that is no longer used") {
    RenderGraph graph;
    const ImageId output = graph.import_image("output", imported_output());
    const ImageId world = graph.create_image("world", color_target);
    const ImageId overlay = graph.create_image("overlay", color_target);
    graph.add_pass("draw world", {{world, uses::color_attachment}}, record_nothing);
    graph.add_pass(
        "deliver world", {{world, uses::transfer_source}, {output, uses::transfer_destination}}, record_nothing);
    graph.add_pass("draw overlay", {{overlay, uses::color_attachment}}, record_nothing);
    graph.add_pass(
        "deliver overlay", {{overlay, uses::transfer_source}, {output, uses::transfer_destination}}, record_nothing);

    const GraphPlan plan = plan_graph(graph);

    CHECK(plan.transients.descriptions.size() == 1);
    CHECK(plan.transients.slot_of_image.at(world.index) == plan.transients.slot_of_image.at(overlay.index));
}

TEST_CASE("an image that takes over a slot discards the old contents but waits for the previous user") {
    RenderGraph graph;
    const ImageId output = graph.import_image("output", imported_output());
    const ImageId world = graph.create_image("world", color_target);
    const ImageId overlay = graph.create_image("overlay", color_target);
    graph.add_pass("draw world", {{world, uses::color_attachment}}, record_nothing);
    graph.add_pass(
        "deliver world", {{world, uses::transfer_source}, {output, uses::transfer_destination}}, record_nothing);
    graph.add_pass("draw overlay", {{overlay, uses::color_attachment}}, record_nothing);

    const GraphPlan plan = plan_graph(graph);

    REQUIRE(plan.passes.at(2).transitions.size() == 1);
    const LayoutChange& takeover = plan.passes.at(2).transitions.front().change;
    CHECK(takeover.from == VK_IMAGE_LAYOUT_UNDEFINED);
    CHECK(takeover.source_stage == (uses::color_attachment.stages | uses::transfer_source.stages));
    CHECK(takeover.source_access == uses::color_attachment.access);
}

TEST_CASE("images with different descriptions never share a slot") {
    RenderGraph graph;
    const ImageId color = graph.create_image("color", color_target);
    const ImageId depth = graph.create_image("depth", depth_target);
    graph.add_pass("draw color", {{color, uses::color_attachment}}, record_nothing);
    graph.add_pass("draw depth", {{depth, uses::depth_attachment}}, record_nothing);

    const GraphPlan plan = plan_graph(graph);

    CHECK(plan.transients.descriptions.size() == 2);
}

TEST_CASE("an imported image takes no slot") {
    RenderGraph graph;
    const ImageId output = graph.import_image("output", imported_output());
    graph.add_pass("fill", {{output, uses::transfer_destination}}, record_nothing);

    const GraphPlan plan = plan_graph(graph);

    CHECK(plan.transients.descriptions.empty());
    CHECK_FALSE(plan.transients.slot_of_image.at(output.index).has_value());
}
