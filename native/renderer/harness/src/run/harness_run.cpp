#include "run/harness_run.hpp"

#include <array>
#include <cstddef>
#include <optional>
#include <utility>

#include "device/device.hpp"
#include "device/vulkan_error.hpp"
#include "frame/frame_targets.hpp"
#include "graph/planned_frame.hpp"
#include "presentation/headless_surface.hpp"
#include "presentation/presenter.hpp"
#include "scene/proof_scene.hpp"

namespace minemod::harness {

namespace {

constexpr std::size_t presentation_slots = 1;
constexpr std::size_t first_slot = 0;

struct RenderedFrames {
    renderer::DeviceDescription device;
    VkFormat presentation_format;
    renderer::FramePixels frame;
};

RenderedFrames render_and_present(const renderer::Instance& instance, const HarnessOptions& options) {
    const renderer::HeadlessSurface surface(instance.get());
    const std::array device_extensions{VK_KHR_SWAPCHAIN_EXTENSION_NAME};
    const renderer::SelectedDevice selected = renderer::select_device(
        instance.get(),
        renderer::DeviceRequirements{.extensions = device_extensions, .presentation_surface = surface.get()});
    const renderer::Device device(selected, device_extensions);
    const renderer::FrameTargets targets(device, options.extent);
    const ProofScene scene(device);
    const renderer::PlannedFrame scene_frame = scene.plan_frame(device, targets);
    renderer::Presenter presenter(device, surface.get(), options.extent, presentation_slots);
    device.submit([&](VkCommandBuffer commands) { scene_frame.record(commands); });
    renderer::FramePixels frame = renderer::read_back_color(device, targets.color());
    for (std::uint32_t presented = 0; presented < options.presented_frames; ++presented) {
        const std::optional<renderer::PresentationTarget> target = presenter.acquire(first_slot);
        if (!target) {
            throw renderer::UnsupportedSystem("the headless surface accepted no frame");
        }
        device.submit(
            [&](VkCommandBuffer commands) {
                scene_frame.record(commands);
                presenter.record_copy(commands, targets.color(), *target);
            },
            target->semaphores);
        presenter.present(*target);
    }
    return RenderedFrames{selected.description, presenter.format(), std::move(frame)};
}

}

HarnessReport run_harness(const HarnessOptions& options) {
    const renderer::Instance instance(options.validation, renderer::headless_surface_extensions);
    RenderedFrames rendered = render_and_present(instance, options);
    return HarnessReport{
        .device = std::move(rendered.device),
        .validation = options.validation,
        .presentation_format = rendered.presentation_format,
        .presented_frames = options.presented_frames,
        .frame = std::move(rendered.frame),
        .validation_messages = instance.validation_messages(),
    };
}

}
