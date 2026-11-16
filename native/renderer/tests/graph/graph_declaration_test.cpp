#include <doctest.h>

#include "graph/graph_error.hpp"
#include "graph/graph_plan.hpp"
#include "graph/render_graph.hpp"
#include "support/graph_targets.hpp"

namespace {

using namespace minemod::renderer;
using namespace minemod::renderer::test_support;

constexpr ImageUse use_without_layout{
    VK_IMAGE_LAYOUT_UNDEFINED, VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT, VK_ACCESS_2_TRANSFER_READ_BIT};

}

TEST_CASE("a pass must record something") {
    RenderGraph graph;
    const ImageId target = graph.create_image("target", color_target);

    CHECK_THROWS_WITH_AS(
        graph.add_pass("silent", {{target, uses::color_attachment}}, RecordPass{}),
        doctest::Contains("'silent'"),
        GraphError);
}

TEST_CASE("a pass can only use images declared by its graph") {
    RenderGraph graph;

    CHECK_THROWS_WITH_AS(
        graph.add_pass("stray", {{ImageId{3}, uses::color_attachment}}, record_nothing),
        doctest::Contains("'stray'"),
        GraphError);
}

TEST_CASE("a pass names each image once") {
    RenderGraph graph;
    const ImageId target = graph.create_image("target", color_target);

    CHECK_THROWS_WITH_AS(
        graph.add_pass(
            "feedback", {{target, uses::color_attachment}, {target, uses::sampled_by_fragment_shader}}, record_nothing),
        doctest::Contains("'target' more than once"),
        GraphError);
}

TEST_CASE("every use states the layout it needs") {
    RenderGraph graph;
    const ImageId target = graph.create_image("target", color_target);

    CHECK_THROWS_WITH_AS(
        graph.add_pass("vague", {{target, use_without_layout}}, record_nothing),
        doctest::Contains("'target' without a layout"),
        GraphError);
    CHECK_THROWS_WITH_AS(
        graph.import_image(
            "output",
            ImportedImage{
                VK_NULL_HANDLE, VK_NULL_HANDLE, target_extent, VK_IMAGE_ASPECT_COLOR_BIT, uses::untouched, uses::untouched}),
        doctest::Contains("'output'"),
        GraphError);
}

TEST_CASE("a pass depends neither on itself nor on a pass the graph did not declare") {
    RenderGraph graph;
    const ImageId target = graph.create_image("target", color_target);
    const PassId clear = graph.add_pass("clear", {{target, uses::color_attachment}}, record_nothing);

    CHECK_THROWS_WITH_AS(graph.add_dependency(clear, clear), doctest::Contains("'clear'"), GraphError);
    CHECK_THROWS_AS(graph.add_dependency(clear, PassId{9}), GraphError);
    CHECK_THROWS_AS(graph.add_dependency(PassId{9}, clear), GraphError);
}

TEST_CASE("an image no pass uses is rejected by name") {
    RenderGraph graph;
    const ImageId target = graph.create_image("target", color_target);
    graph.create_image("forgotten", depth_target);
    graph.add_pass("clear", {{target, uses::color_attachment}}, record_nothing);

    CHECK_THROWS_WITH_AS(plan_graph(graph), doctest::Contains("'forgotten'"), GraphError);
}

TEST_CASE("a pass cannot read an image before any pass writes it") {
    RenderGraph graph;
    const ImageId target = graph.create_image("target", color_target);
    graph.add_pass("copy", {{target, uses::transfer_source}}, record_nothing);

    CHECK_THROWS_WITH_AS(
        plan_graph(graph), doctest::Contains("'copy' reads the image 'target' before any pass writes it"), GraphError);
}
