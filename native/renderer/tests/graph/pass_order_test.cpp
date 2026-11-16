#include <vector>

#include <doctest.h>

#include "graph/graph_error.hpp"
#include "graph/pass_order.hpp"
#include "graph/render_graph.hpp"
#include "support/graph_targets.hpp"

namespace {

using namespace minemod::renderer;
using namespace minemod::renderer::test_support;

}

TEST_CASE("passes that share nothing run in the order they were declared") {
    RenderGraph graph;
    const ImageId near_target = graph.create_image("near target", color_target);
    const ImageId far_target = graph.create_image("far target", color_target);
    const PassId near = graph.add_pass("near", {{near_target, uses::color_attachment}}, record_nothing);
    const PassId far = graph.add_pass("far", {{far_target, uses::color_attachment}}, record_nothing);

    CHECK(order_passes(graph) == std::vector{near, far});
}

TEST_CASE("a declared dependency runs a pass after the one it depends on") {
    RenderGraph graph;
    const ImageId near_target = graph.create_image("near target", color_target);
    const ImageId far_target = graph.create_image("far target", color_target);
    const PassId near = graph.add_pass("near", {{near_target, uses::color_attachment}}, record_nothing);
    const PassId far = graph.add_pass("far", {{far_target, uses::color_attachment}}, record_nothing);
    graph.add_dependency(near, far);

    CHECK(order_passes(graph) == std::vector{far, near});
}

TEST_CASE("a pass moved by a dependency still leaves readers between the writes they were declared between") {
    RenderGraph graph;
    const ImageId target = graph.create_image("target", color_target);
    const ImageId overlay_target = graph.create_image("overlay target", color_target);
    const PassId draw = graph.add_pass("draw", {{target, uses::color_attachment}}, record_nothing);
    const PassId overlay = graph.add_pass("overlay", {{overlay_target, uses::color_attachment}}, record_nothing);
    const PassId copy = graph.add_pass("copy", {{target, uses::transfer_source}}, record_nothing);
    const PassId redraw = graph.add_pass("redraw", {{target, uses::color_attachment}}, record_nothing);
    graph.add_dependency(overlay, redraw);

    CHECK(order_passes(graph) == std::vector{draw, copy, redraw, overlay});
}

TEST_CASE("a dependency that contradicts the image accesses is rejected as a cycle naming the passes") {
    RenderGraph graph;
    const ImageId target = graph.create_image("target", color_target);
    const PassId clear = graph.add_pass("clear", {{target, uses::color_attachment}}, record_nothing);
    const PassId copy = graph.add_pass("copy", {{target, uses::transfer_source}}, record_nothing);
    graph.add_dependency(clear, copy);

    CHECK_THROWS_WITH_AS(order_passes(graph), doctest::Contains("'clear' after 'copy' after 'clear'"), GraphError);
}

TEST_CASE("a cycle is reported without the passes that merely wait behind it") {
    RenderGraph graph;
    const ImageId near_target = graph.create_image("near target", color_target);
    const ImageId far_target = graph.create_image("far target", color_target);
    const ImageId last_target = graph.create_image("last target", color_target);
    const PassId last = graph.add_pass("last", {{last_target, uses::color_attachment}}, record_nothing);
    const PassId near = graph.add_pass("near", {{near_target, uses::color_attachment}}, record_nothing);
    const PassId far = graph.add_pass("far", {{far_target, uses::color_attachment}}, record_nothing);
    graph.add_dependency(near, far);
    graph.add_dependency(far, near);
    graph.add_dependency(last, near);

    CHECK_THROWS_WITH_AS(order_passes(graph), doctest::Contains(": 'near' after 'far' after 'near'"), GraphError);
}
