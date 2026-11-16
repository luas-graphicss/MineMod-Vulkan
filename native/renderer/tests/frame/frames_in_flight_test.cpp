#include <cstddef>
#include <cstdint>
#include <cstring>
#include <stdexcept>
#include <vector>

#include <doctest.h>

#include "frame/frames_in_flight.hpp"
#include "lifetime/retirement_queue.hpp"
#include "resources/buffer.hpp"
#include "support/tracked_resource.hpp"
#include "support/validated_device.hpp"

namespace {

using namespace minemod::renderer;
using namespace minemod::renderer::test_support;

constexpr std::uint32_t frame_total = 5;
constexpr VkDeviceSize mark_bytes = sizeof(std::uint32_t);

std::uint32_t mark_of(const std::vector<std::byte>& marks, std::uint32_t frame) {
    std::uint32_t mark = 0;
    std::memcpy(&mark, marks.data() + frame * mark_bytes, mark_bytes);
    return mark;
}

}

TEST_CASE("frames are submitted without waiting and complete in submission order") {
    const ValidatedDevice gpu;
    const Buffer marks(gpu.device(), frame_total * mark_bytes, VK_BUFFER_USAGE_TRANSFER_DST_BIT, host_memory);
    {
        FramesInFlight frames(gpu.device(), 2);
        for (std::uint32_t index = 0; index < frame_total; ++index) {
            const FramesInFlight::Frame frame = frames.begin();
            vkCmdFillBuffer(frame.commands, marks.get(), frame.number * mark_bytes, mark_bytes, index + 1);
            frames.submit();
        }
        CHECK(frames.completed_frame() == std::uint64_t{2});
    }

    const std::vector<std::byte> stored = marks.read();
    for (std::uint32_t index = 0; index < frame_total; ++index) {
        CHECK(mark_of(stored, index) == index + 1);
    }
    CHECK(gpu.validation_text() == "");
}

TEST_CASE("a resource retired in a frame is released once that frame completed") {
    const ValidatedDevice gpu;
    std::vector<int> released;
    RetirementQueue retirements;
    FramesInFlight frames(gpu.device(), 2);

    retirements.retire(frames.begin().number, TrackedResource(released, 1));
    frames.submit();
    frames.begin();
    frames.submit();
    CHECK_FALSE(frames.completed_frame().has_value());

    frames.begin();
    retirements.release_through(frames.completed_frame().value());
    frames.submit();

    CHECK(released == std::vector<int>{1});
    CHECK(gpu.validation_text() == "");
}

TEST_CASE("a frame must be submitted before the next one begins") {
    const ValidatedDevice gpu;
    FramesInFlight frames(gpu.device(), 2);
    CHECK_THROWS_AS(frames.submit(), std::logic_error);

    frames.begin();

    CHECK_THROWS_AS(frames.begin(), std::logic_error);
    CHECK_THROWS_AS(FramesInFlight(gpu.device(), 0), std::invalid_argument);
}
