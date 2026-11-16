#include <optional>
#include <vector>

#include <doctest.h>

#include "graph/graph_plan.hpp"
#include "graph/render_graph.hpp"
#include "support/graph_targets.hpp"

namespace {

using namespace minemod::renderer;
using namespace minemod::renderer::test_support;

constexpr ImageUse sampled_by_compute_shader{
    VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
    VK_PIPELINE_STAGE_2_COMPUTE_SHADER_BIT,
    VK_ACCESS_2_SHADER_SAMPLED_READ_BIT,
};

constexpr ImageUse acquired_for_transfer{
    VK_IMAGE_LAYOUT_UNDEFINED, VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT, VK_ACCESS_2_NONE};

constexpr ImageUse presentable{VK_IMAGE_LAYOUT_PRESENT_SRC_KHR, VK_PIPELINE_STAGE_2_NONE, VK_ACCESS_2_NONE};

ImportedImage imported_between(const ImageUse& entry, const std::optional<ImageUse>& exit) {
    return ImportedImage{VK_NULL_HANDLE, VK_NULL_HANDLE, target_extent, VK_IMAGE_ASPECT_COLOR_BIT, entry, exit};
}

const LayoutChange& only_change(const std::vector<ImageTransition>& transitions) {
    REQUIRE(transitions.size() == 1);
    return transitions.front().change;
}

}

TEST_CASE("the first write to a transient image discards its contents and waits for nothing") {
    RenderGraph graph;
    const ImageId target = graph.create_image("target", color_target);
    graph.add_pass("clear", {{target, uses::color_attachment}}, record_nothing);

    const GraphPlan plan = plan_graph(graph);

    const LayoutChange& change = only_change(plan.passes.at(0).transitions);
    CHECK(change.from == VK_IMAGE_LAYOUT_UNDEFINED);
    CHECK(change.to == VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
    CHECK(change.source_stage == VK_PIPELINE_STAGE_2_NONE);
    CHECK(change.source_access == VK_ACCESS_2_NONE);
    CHECK(change.destination_stage == uses::color_attachment.stages);
    CHECK(change.destination_access == uses::color_attachment.access);
}

TEST_CASE("a reader waits for the writer and moves the image into its own layout") {
    RenderGraph graph;
    const ImageId target = graph.create_image("target", color_target);
    graph.add_pass("clear", {{target, uses::color_attachment}}, record_nothing);
    graph.add_pass("copy", {{target, uses::transfer_source}}, record_nothing);

    const GraphPlan plan = plan_graph(graph);

    const LayoutChange& change = only_change(plan.passes.at(1).transitions);
    CHECK(change.from == VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
    CHECK(change.to == VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL);
    CHECK(change.source_stage == uses::color_attachment.stages);
    CHECK(change.source_access == uses::color_attachment.access);
    CHECK(change.destination_stage == uses::transfer_source.stages);
    CHECK(change.destination_access == uses::transfer_source.access);
}

TEST_CASE("a second reader at the same stage needs no transition of its own") {
    RenderGraph graph;
    const ImageId target = graph.create_image("target", color_target);
    graph.add_pass("clear", {{target, uses::color_attachment}}, record_nothing);
    graph.add_pass("terrain", {{target, uses::sampled_by_fragment_shader}}, record_nothing);
    graph.add_pass("entities", {{target, uses::sampled_by_fragment_shader}}, record_nothing);

    const GraphPlan plan = plan_graph(graph);

    CHECK(plan.passes.at(1).transitions.size() == 1);
    CHECK(plan.passes.at(2).transitions.empty());
}

TEST_CASE("a reader at another stage waits for the writer in the layout already set") {
    RenderGraph graph;
    const ImageId target = graph.create_image("target", color_target);
    graph.add_pass("clear", {{target, uses::color_attachment}}, record_nothing);
    graph.add_pass("terrain", {{target, uses::sampled_by_fragment_shader}}, record_nothing);
    graph.add_pass("culling", {{target, sampled_by_compute_shader}}, record_nothing);

    const GraphPlan plan = plan_graph(graph);

    const LayoutChange& change = only_change(plan.passes.at(2).transitions);
    CHECK(change.from == VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
    CHECK(change.to == VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
    CHECK(change.source_stage == (uses::color_attachment.stages | uses::sampled_by_fragment_shader.stages));
    CHECK(change.source_access == uses::color_attachment.access);
    CHECK(change.destination_stage == sampled_by_compute_shader.stages);
    CHECK(change.destination_access == sampled_by_compute_shader.access);
}

TEST_CASE("a writer waits for the previous writer and every reader since") {
    RenderGraph graph;
    const ImageId target = graph.create_image("target", color_target);
    graph.add_pass("clear", {{target, uses::color_attachment}}, record_nothing);
    graph.add_pass("terrain", {{target, uses::sampled_by_fragment_shader}}, record_nothing);
    graph.add_pass("culling", {{target, sampled_by_compute_shader}}, record_nothing);
    graph.add_pass("redraw", {{target, uses::color_attachment}}, record_nothing);

    const GraphPlan plan = plan_graph(graph);

    const LayoutChange& change = only_change(plan.passes.at(3).transitions);
    CHECK(change.from == VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
    CHECK(change.to == VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
    CHECK(
        change.source_stage
        == (uses::color_attachment.stages | uses::sampled_by_fragment_shader.stages
            | sampled_by_compute_shader.stages));
    CHECK(change.source_access == uses::color_attachment.access);
}

TEST_CASE("an imported image starts from its entry scope and leaves in its exit use") {
    RenderGraph graph;
    const ImageId surface = graph.import_image("surface", imported_between(acquired_for_transfer, presentable));
    graph.add_pass("blit", {{surface, uses::transfer_destination}}, record_nothing);

    const GraphPlan plan = plan_graph(graph);

    const LayoutChange& entered = only_change(plan.passes.at(0).transitions);
    CHECK(entered.from == VK_IMAGE_LAYOUT_UNDEFINED);
    CHECK(entered.to == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
    CHECK(entered.source_stage == VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT);
    CHECK(entered.source_access == VK_ACCESS_2_NONE);

    const LayoutChange& left = only_change(plan.closing_transitions);
    CHECK(left.from == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
    CHECK(left.to == VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);
    CHECK(left.source_stage == VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT);
    CHECK(left.source_access == VK_ACCESS_2_TRANSFER_WRITE_BIT);
    CHECK(left.destination_stage == VK_PIPELINE_STAGE_2_NONE);
    CHECK(left.destination_access == VK_ACCESS_2_NONE);
}

TEST_CASE("an imported image already in the use every pass needs is left alone") {
    RenderGraph graph;
    const ImageId atlas = graph.import_image(
        "atlas", imported_between(uses::sampled_by_fragment_shader, uses::sampled_by_fragment_shader));
    graph.add_pass("terrain", {{atlas, uses::sampled_by_fragment_shader}}, record_nothing);

    const GraphPlan plan = plan_graph(graph);

    CHECK(plan.passes.at(0).transitions.empty());
    CHECK(plan.closing_transitions.empty());
}

TEST_CASE("an imported image without an exit use stays where its last pass left it") {
    RenderGraph graph;
    const ImageId depth = graph.import_image("depth", imported_between(uses::untouched, std::nullopt));
    graph.add_pass("draw", {{depth, uses::depth_attachment}}, record_nothing);

    const GraphPlan plan = plan_graph(graph);

    CHECK(plan.passes.at(0).transitions.size() == 1);
    CHECK(plan.closing_transitions.empty());
}
