#include <array>
#include <cstddef>
#include <optional>
#include <string>
#include <utility>

#include <doctest.h>

#include "frame/frame_rendering.hpp"
#include "frame/frame_targets.hpp"
#include "frame/frames_in_flight.hpp"
#include "graph/image_use.hpp"
#include "graph/planned_frame.hpp"
#include "graph/render_graph.hpp"
#include "presentation/frame_lifecycle.hpp"
#include "presentation/presenter.hpp"
#include "support/presenting_device.hpp"

namespace {

using namespace minemod::renderer;
using namespace minemod::renderer::test_support;

constexpr VkExtent2D frame_extent{32, 32};
constexpr VkExtent2D window_extent{64, 48};
constexpr VkExtent2D wider_window_extent{96, 48};
constexpr VkExtent2D minimized_extent{0, 0};
constexpr std::size_t frames_in_flight = 2;
constexpr int resize_rounds = 200;
constexpr std::array<float, 4> gray{0.5f, 0.5f, 0.5f, 1.0f};

class PresentedFrames {
public:
    PresentedFrames()
        : targets_(gpu_.device(), frame_extent),
          frames_(gpu_.device(), frames_in_flight),
          presenter_(gpu_.device(), gpu_.surface(), window_extent, frames_in_flight) {}

    Presenter& presenter() noexcept {
        return presenter_;
    }

    std::optional<VkExtent2D> present_frame() {
        const FramesInFlight::Frame frame = frames_.begin();
        const std::size_t slot = frame.number % frames_in_flight;
        planned_[slot].emplace(gpu_.device(), cleared_frame());
        planned_[slot]->record(frame.commands);
        const std::optional<PresentationTarget> target = presenter_.acquire(slot);
        if (!target) {
            frames_.submit();
            return std::nullopt;
        }
        presenter_.record_copy(frame.commands, targets_.color(), *target);
        frames_.submit(target->semaphores);
        presenter_.present(*target);
        return target->extent;
    }

    std::string validation_text() const {
        return gpu_.validation_text();
    }

private:
    RenderGraph cleared_frame() const {
        RenderGraph graph;
        const ImageId color = graph.import_image(
            "frame color",
            ImportedImage{
                targets_.color().get(),
                targets_.color().view(),
                frame_extent,
                VK_IMAGE_ASPECT_COLOR_BIT,
                discarded_after(uses::transfer_source),
                uses::transfer_source,
            });
        const ImageId depth = graph.import_image(
            "frame depth",
            ImportedImage{
                targets_.depth().get(),
                targets_.depth().view(),
                frame_extent,
                VK_IMAGE_ASPECT_DEPTH_BIT,
                discarded_after(uses::depth_attachment),
                std::nullopt,
            });
        graph.add_pass(
            "clear",
            {{color, uses::color_attachment}, {depth, uses::depth_attachment}},
            [color, depth](VkCommandBuffer commands, const GraphImages& images) {
                begin_frame_rendering(commands, FrameAttachments{images.at(color), images.at(depth)}, gray);
                vkCmdEndRendering(commands);
            });
        return graph;
    }

    PresentingDevice gpu_;
    FrameTargets targets_;
    std::array<std::optional<PlannedFrame>, frames_in_flight> planned_;
    FramesInFlight frames_;
    Presenter presenter_;
};

void check_extent(const std::optional<VkExtent2D>& presented, VkExtent2D expected) {
    REQUIRE(presented.has_value());
    CHECK(presented->width == expected.width);
    CHECK(presented->height == expected.height);
}

}

TEST_CASE("frames in flight are presented on a surface without waiting for each one") {
    PresentedFrames frames;

    for (int frame = 0; frame < 6; ++frame) {
        check_extent(frames.present_frame(), window_extent);
    }

    CHECK(frames.presenter().state() == FrameState::running);
    CHECK(frames.validation_text() == "");
}

TEST_CASE("a resized surface gets a swapchain of its new extent before the next frame") {
    PresentedFrames frames;
    check_extent(frames.present_frame(), window_extent);

    frames.presenter().resize(wider_window_extent);
    CHECK(frames.presenter().state() == FrameState::awaiting_swapchain);

    check_extent(frames.present_frame(), wider_window_extent);
    CHECK(frames.presenter().state() == FrameState::running);
    CHECK(frames.validation_text() == "");
}

TEST_CASE("a surface without area presents nothing until it has area again") {
    PresentedFrames frames;
    check_extent(frames.present_frame(), window_extent);

    frames.presenter().resize(minimized_extent);
    CHECK_FALSE(frames.present_frame().has_value());
    CHECK(frames.presenter().state() == FrameState::suspended);

    frames.presenter().resize(window_extent);
    check_extent(frames.present_frame(), window_extent);
    CHECK(frames.validation_text() == "");
}

TEST_CASE("hundreds of resizes with frames in flight leave no validation message") {
    PresentedFrames frames;

    for (int round = 0; round < resize_rounds; ++round) {
        const VkExtent2D extent = round % 2 == 0 ? wider_window_extent : window_extent;
        frames.presenter().resize(extent);
        check_extent(frames.present_frame(), extent);
    }

    CHECK(frames.validation_text() == "");
}
