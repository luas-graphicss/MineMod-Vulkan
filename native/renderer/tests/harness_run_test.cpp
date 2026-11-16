#include <cstdint>
#include <string>
#include <vector>

#include <doctest.h>

#include "run/harness_run.hpp"
#include "scene/proof_scene_colors.hpp"

namespace doctest {

template <>
struct StringMaker<minemod::renderer::Pixel> {
    static String convert(const minemod::renderer::Pixel& pixel) {
        const std::string text = "rgba(" + std::to_string(pixel.red) + ", " + std::to_string(pixel.green) + ", "
            + std::to_string(pixel.blue) + ", " + std::to_string(pixel.alpha) + ")";
        return String(text.c_str());
    }
};

}

namespace {

using namespace minemod::harness;
using minemod::renderer::Validation;

constexpr VkExtent2D frame_extent{64, 64};
constexpr std::uint32_t presented_frames = 3;
constexpr int repeated_runs = 5;

HarnessReport validated_run() {
    return run_harness(
        HarnessOptions{.validation = Validation::required, .extent = frame_extent, .presented_frames = presented_frames});
}

std::string joined(const std::vector<std::string>& messages) {
    std::string text;
    for (const std::string& message : messages) {
        text += message + '\n';
    }
    return text;
}

}

TEST_CASE("the proof scene reads back the clear color, each texel and the nearer triangle") {
    const HarnessReport report = validated_run();

    CHECK(report.frame.at(2, 2) == proof_clear_color);
    CHECK(report.frame.at(16, 16) == checker_texels[0]);
    CHECK(report.frame.at(48, 16) == checker_texels[1]);
    CHECK(report.frame.at(16, 48) == checker_texels[2]);
    CHECK(report.frame.at(48, 48) == checker_texels[3]);
    CHECK(report.frame.at(32, 30) == proof_triangle_color);
}

TEST_CASE("the harness presents every requested frame without validation messages") {
    const HarnessReport report = validated_run();

    CHECK(report.presented_frames == presented_frames);
    CHECK(report.presentation_format != VK_FORMAT_UNDEFINED);
    CHECK(joined(report.validation_messages) == "");
}

TEST_CASE("the report names a Vulkan 1.3 device and its driver") {
    const HarnessReport report = validated_run();

    CHECK_FALSE(report.device.name.empty());
    CHECK_FALSE(report.device.driver.empty());
    CHECK(report.device.api_version >= VK_API_VERSION_1_3);
}

TEST_CASE("repeated runs release every device object") {
    for (int run = 0; run < repeated_runs; ++run) {
        CHECK(joined(validated_run().validation_messages) == "");
    }
}
