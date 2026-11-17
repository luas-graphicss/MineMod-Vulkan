#include <cstdint>
#include <stdexcept>

#include <doctest.h>

#include "device/device_handle.hpp"
#include "frame/frame_readback.hpp"
#include "handoff/shared_resource_handoff.hpp"
#include "harness/scene_image.hpp"
#include "harness/shared_scene_host.hpp"

namespace {

using namespace minemod;
using namespace minemod::interop;
using namespace minemod::interop::harness;

struct Texel {
    std::uint32_t x;
    std::uint32_t y;
};

constexpr VkExtent2D scene_extent{64, 32};
constexpr VkRect2D whole_scene{{0, 0}, scene_extent};
constexpr VkRect2D left_half{{0, 0}, {scene_extent.width / 2, scene_extent.height}};
constexpr Texel inside_left_half{8, 8};
constexpr Texel inside_right_half{56, 8};
constexpr int round_trips = 200;
constexpr VkClearValue renderer_blue{.color = {.float32 = {0.0F, 0.0F, 1.0F, 1.0F}}};
constexpr VkClearValue far_depth{.depthStencil = {1.0F, 0}};
constexpr renderer::Pixel blue{0, 0, 255, 255};
constexpr SceneImageDescription color_target{SceneImageKind::color, scene_extent, VK_FORMAT_R8G8B8A8_UNORM};
constexpr SceneImageDescription depth_target{
    SceneImageKind::depth_stencil, scene_extent, VK_FORMAT_D32_SFLOAT_S8_UINT};

renderer::DeviceHandle<VkSemaphore> create_semaphore(const renderer::Device& device) {
    const VkSemaphoreCreateInfo create_info{.sType = VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO};
    return renderer::create_device_handle<VkSemaphore>(
        device.get(), create_info, vkCreateSemaphore, vkDestroySemaphore, "creating a handoff semaphore");
}

const SceneImage* depth_the_device_shares(SharedSceneHost& host) {
    if (!host.support_for(depth_target.kind, depth_target.format).exportable) {
        return nullptr;
    }
    return &host.image(host.create_image(depth_target));
}

struct SharedScene {
    explicit SharedScene(SharedSceneHost& host)
        : color(host.image(host.create_image(color_target))),
          depth(depth_the_device_shares(host)),
          renderer_done(create_semaphore(host.device())),
          provider_done(create_semaphore(host.device())),
          resources{{color.handoff_use()}, {}},
          semaphores{renderer_done.get(), provider_done.get()} {
        if (depth != nullptr) {
            resources.images.push_back(depth->handoff_use());
        }
    }

    const SceneImage& color;
    const SceneImage* depth;
    renderer::DeviceHandle<VkSemaphore> renderer_done;
    renderer::DeviceHandle<VkSemaphore> provider_done;
    HandoffResources resources;
    HandoffSemaphores semaphores;
};

std::uint8_t shade_of(int round_trip) {
    return static_cast<std::uint8_t>(round_trip % 256);
}

void draw_as_the_provider(const renderer::Device& device, const SharedScene& scene, std::uint8_t green_shade) {
    const OwnershipTransfer acquire = OwnershipTransfer::acquire_for(device.queue_family(), scene.resources);
    const OwnershipTransfer release = OwnershipTransfer::release_from(device.queue_family(), scene.resources);
    const VkClearValue shade{.color = {.float32 = {0.0F, static_cast<float>(green_shade) / 255.0F, 0.0F, 1.0F}}};

    device.submit(
        [&](VkCommandBuffer commands) {
            acquire.record(commands);
            scene.color.record_clear(commands, left_half, shade);
            release.record(commands);
        },
        {
            .wait = scene.semaphores.renderer_done,
            .wait_stage = VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT,
            .signal = scene.semaphores.provider_done,
        });
}

}

TEST_CASE("a handoff needs a semaphore of its own for each direction") {
    SharedSceneHost host;
    const SharedScene scene(host);
    const VkSemaphore only = scene.semaphores.renderer_done;

    CHECK_THROWS_AS(
        SharedResourceHandoff(host.device(), scene.resources, {only, VK_NULL_HANDLE}), std::invalid_argument);
    CHECK_THROWS_AS(
        SharedResourceHandoff(host.device(), scene.resources, {VK_NULL_HANDLE, only}), std::invalid_argument);
    CHECK_THROWS_AS(SharedResourceHandoff(host.device(), scene.resources, {only, only}), std::invalid_argument);
}

TEST_CASE("shared resources are released once and reclaimed only after a release") {
    SharedSceneHost host;
    const SharedScene scene(host);
    SharedResourceHandoff handoff(host.device(), scene.resources, scene.semaphores);

    CHECK(handoff.owner() == ResourceOwner::renderer);
    CHECK_THROWS_AS(handoff.reclaim_for_renderer(), std::logic_error);

    handoff.release_to_provider();
    CHECK(handoff.owner() == ResourceOwner::provider);
    CHECK_THROWS_AS(handoff.release_to_provider(), std::logic_error);

    draw_as_the_provider(host.device(), scene, shade_of(0));
    handoff.reclaim_for_renderer();
    CHECK(handoff.owner() == ResourceOwner::renderer);
    CHECK(handoff.completed_round_trips() == 1);
    CHECK(host.validation_text() == "");
}

TEST_CASE("the renderer reads what the provider drew between a release and a reclaim") {
    SharedSceneHost host;
    const SharedScene scene(host);
    SharedResourceHandoff handoff(host.device(), scene.resources, scene.semaphores);

    for (int round_trip = 0; round_trip < round_trips; ++round_trip) {
        scene.color.clear(whole_scene, renderer_blue);
        if (scene.depth != nullptr) {
            scene.depth->clear(whole_scene, far_depth);
        }
        handoff.release_to_provider();
        draw_as_the_provider(host.device(), scene, shade_of(round_trip));
        handoff.reclaim_for_renderer();

        const renderer::FramePixels pixels = scene.color.read_color();
        const renderer::Pixel drawn_by_the_provider{0, shade_of(round_trip), 0, 255};
        REQUIRE(pixels.at(inside_left_half.x, inside_left_half.y) == drawn_by_the_provider);
        REQUIRE(pixels.at(inside_right_half.x, inside_right_half.y) == blue);
    }

    CHECK(handoff.completed_round_trips() == round_trips);
    CHECK(host.validation_text() == "");
}
