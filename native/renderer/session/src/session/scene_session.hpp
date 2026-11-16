#pragma once

#include <array>
#include <cstddef>
#include <cstdint>
#include <memory>
#include <optional>
#include <span>
#include <vector>

#include <vulkan/vulkan.h>

#include "device/device.hpp"
#include "draw/batch_renderer.hpp"
#include "frame/frame_slot.hpp"
#include "frame/frame_targets.hpp"
#include "frame/frames_in_flight.hpp"
#include "frame/recorded_frame.hpp"
#include "graph/render_graph.hpp"
#include "host/scene_host.hpp"
#include "lifetime/retirement_queue.hpp"
#include "minemod/abi/handle.h"
#include "minemod/abi/result.h"
#include "minemod/scene/scene_session.h"
#include "pipelines/pipeline_cache.hpp"
#include "presentation/present_pacing.hpp"
#include "presentation/presenter.hpp"
#include "presentation/window_surface.hpp"
#include "resources/texture_registry.hpp"
#include "scene/scene_renderer.hpp"
#include "terrain/section_intake.hpp"
#include "terrain/terrain_renderer.hpp"
#include "terrain/terrain_sections.hpp"
#include "textures/sampler_cache.hpp"
#include "textures/texture.hpp"
#include "visibility/section_occlusion.hpp"
#include "work/job_epochs.hpp"

namespace minemod::scene {

inline constexpr std::size_t frames_in_flight = 2;

struct SessionSetup {
    VkExtent2D extent;
    VkSurfaceKHR window_surface;
    SceneLimits limits;
};

class SceneSession {
public:
    SceneSession(std::shared_ptr<const SceneHost> host, const SessionSetup& setup);
    ~SceneSession();

    SceneSession(const SceneSession&) = delete;
    SceneSession& operator=(const SceneSession&) = delete;

    minemod_result execute(std::uint16_t opcode, std::span<const std::byte> payload);
    minemod_result resize(VkExtent2D extent);
    void set_present_pacing(renderer::PresentPacing pacing) noexcept;
    std::uint32_t presentation_state() const noexcept;
    minemod_result read_frame(std::span<std::byte> rgba);
    minemod_scene_counters counters() const noexcept;

private:
    struct SampledTexture {
        VkImageView view;
        VkSampler sampler;
    };

    minemod_result define_texture(std::span<const std::byte> payload);
    minemod_result write_texture(std::span<const std::byte> payload);
    minemod_result destroy_texture(std::span<const std::byte> payload);
    minemod_result publish_section(std::span<const std::byte> payload);
    minemod_result unload_section(std::span<const std::byte> payload);
    minemod_result change_world();
    minemod_result begin_frame(std::span<const std::byte> payload);
    minemod_result set_view(std::span<const std::byte> payload);
    minemod_result set_terrain(std::span<const std::byte> payload);
    minemod_result draw_batch(std::span<const std::byte> payload);
    minemod_result set_batch_option(std::uint16_t opcode, std::span<const std::byte> payload);
    minemod_result connect_section_faces(std::span<const std::byte> payload);
    minemod_result end_frame();

    void upload_waiting_sections();
    void order_translucent_terrain();
    void select_reachable_sections();
    const renderer::FramesInFlight::Frame& recording();
    FrameSlot& recording_slot();
    std::uint64_t last_frame_using_removed_resources() const noexcept;
    std::optional<SampledTexture> sampled(minemod_handle texture);
    VkDescriptorSet bound_texture(minemod_handle texture, const SampledTexture& sampled);
    std::vector<renderer::ImageId> add_texture_writes(renderer::RenderGraph& graph, const FrameSlot& slot);
    void submit_frame(const renderer::FramesInFlight::Frame& frame);

    std::shared_ptr<const SceneHost> host_;
    std::optional<renderer::WindowSurface> surface_;
    renderer::Device device_;
    SceneLimits limits_;
    VkExtent2D extent_;
    renderer::RetirementQueue retirements_;
    std::optional<renderer::FrameTargets> targets_;
    renderer::SamplerCache samplers_;
    renderer::PipelineCache pipelines_;
    renderer::TerrainRenderer terrain_;
    renderer::BatchRenderer batches_;
    renderer::SceneRenderer scene_;
    renderer::JobEpochs epochs_;
    renderer::TerrainSections sections_;
    SectionIntake intake_;
    renderer::SectionOcclusion occlusion_;
    std::optional<std::vector<renderer::SectionPosition>> reachable_;
    TextureRegistry textures_;
    renderer::Texture white_;
    VkSampler white_sampler_;
    std::array<FrameSlot, frames_in_flight> slots_;
    renderer::FramesInFlight frames_;
    std::optional<renderer::Presenter> presenter_;
    std::optional<renderer::FramesInFlight::Frame> recording_;
    std::optional<std::uint64_t> last_submitted_frame_;
    RecordedFrame frame_;
    renderer::WorldPosition camera_{};
    std::uint32_t world_ = 0;
    bool has_drawn_frame_ = false;
    minemod_scene_counters counters_{};
};

}
