#include <stdexcept>

#include <doctest.h>

#include "support/validated_device.hpp"
#include "textures/lightmap.hpp"
#include "textures/sampler_cache.hpp"

namespace {

using namespace minemod::renderer;
using namespace minemod::renderer::test_support;

constexpr SamplerDescription mipmapped_atlas_sampler{
    VK_FILTER_NEAREST,
    VK_FILTER_NEAREST,
    VK_SAMPLER_MIPMAP_MODE_LINEAR,
    VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE,
    5,
};

}

TEST_CASE("an identical description reuses its sampler") {
    const ValidatedDevice gpu;
    SamplerCache cache(gpu.device());

    const VkSampler created = cache.sampler_for(lightmap_sampler);
    const VkSampler reused = cache.sampler_for(lightmap_sampler);

    CHECK(created == reused);
    CHECK(cache.sampler_count() == 1);
    CHECK(gpu.validation_text() == "");
}

TEST_CASE("descriptions that differ get their own samplers") {
    const ValidatedDevice gpu;
    SamplerCache cache(gpu.device());

    const VkSampler lightmap = cache.sampler_for(lightmap_sampler);
    const VkSampler atlas = cache.sampler_for(mipmapped_atlas_sampler);

    CHECK(lightmap != atlas);
    CHECK(cache.sampler_count() == 2);
    CHECK(gpu.validation_text() == "");
}

TEST_CASE("a sampler that covers no mip level is rejected") {
    const ValidatedDevice gpu;
    SamplerCache cache(gpu.device());
    SamplerDescription without_levels = lightmap_sampler;
    without_levels.mip_levels = 0;

    CHECK_THROWS_AS(cache.sampler_for(without_levels), std::invalid_argument);
}
