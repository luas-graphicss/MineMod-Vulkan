#include <array>
#include <cstddef>
#include <stdexcept>

#include <doctest.h>

#include "descriptors/descriptor_layout.hpp"
#include "descriptors/descriptor_pools.hpp"
#include "support/validated_device.hpp"

namespace {

using namespace minemod::renderer;
using namespace minemod::renderer::test_support;

constexpr std::array sampled_texture{
    DescriptorBinding{0, VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, VK_SHADER_STAGE_FRAGMENT_BIT},
};

DescriptorPoolShape two_sets_per_pool(std::size_t max_pools) {
    return DescriptorPoolShape{2, {{VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, 2}}, max_pools};
}

}

TEST_CASE("sets beyond the first pool come from a second pool") {
    const ValidatedDevice gpu;
    const auto layout = create_descriptor_layout(gpu.device(), sampled_texture);
    DescriptorPools pools(gpu.device(), two_sets_per_pool(2));

    const VkDescriptorSet first = pools.allocate(layout.get());
    const VkDescriptorSet second = pools.allocate(layout.get());
    const VkDescriptorSet third = pools.allocate(layout.get());

    CHECK(first != second);
    CHECK(second != third);
    CHECK(pools.pool_count() == 2);
    CHECK(gpu.validation_text() == "");
}

TEST_CASE("a reset hands out sets again from the pools that already exist") {
    const ValidatedDevice gpu;
    const auto layout = create_descriptor_layout(gpu.device(), sampled_texture);
    DescriptorPools pools(gpu.device(), two_sets_per_pool(2));
    for (int set = 0; set < 3; ++set) {
        pools.allocate(layout.get());
    }

    pools.reset();
    for (int set = 0; set < 4; ++set) {
        pools.allocate(layout.get());
    }

    CHECK(pools.pool_count() == 2);
    CHECK(gpu.validation_text() == "");
}

TEST_CASE("the pools refuse to grow past their limit") {
    const ValidatedDevice gpu;
    const auto layout = create_descriptor_layout(gpu.device(), sampled_texture);
    DescriptorPools pools(gpu.device(), two_sets_per_pool(1));
    pools.allocate(layout.get());
    pools.allocate(layout.get());

    CHECK_THROWS_AS(pools.allocate(layout.get()), std::length_error);
    CHECK(pools.pool_count() == 1);
}
