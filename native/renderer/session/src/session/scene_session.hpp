#pragma once

#include <array>
#include <cstddef>
#include <cstdint>
#include <memory>
#include <optional>
#include <span>
#include <string>
#include <string_view>
#include <vector>

#include <vulkan/vulkan.h>

#include "device/device.hpp"
#include "draw/batch_renderer.hpp"
#include "effects/frame_blur.hpp"
#include "frame/frame_slot.hpp"
#include "frame/frames_in_flight.hpp"
#include "frame/recorded_frame.hpp"
#include "graph/render_graph.hpp"
#include "host/scene_host.hpp"
#include "lifetime/retirement_queue.hpp"
#include "metrics/session_metrics.hpp"
#include "minemod/abi/handle.h"
#include "minemod/abi/result.h"
#include "minemod/scene/scene_session.h"
#include "pipelines/pipeline_cache.hpp"
#include "presentation/present_pacing.hpp"
#include "presentation/presenter.hpp"
#include "presentation/window_surface.hpp"
#include "reload/program_reload.hpp"
#include "resources/texture_registry.hpp"
#include "scene/scene_renderer.hpp"
#include "sharing/session_sharing.hpp"
#include "targets/session_targets.hpp"
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
    bool shares_attachments;
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

    minemod_result begin_reload();
    minemod_result stage_program(
        std::uint32_t program, std::span<const std::uint32_t> vertex, std::span<const std::uint32_t> fragment);
    minemod_result commit_reload();
    minemod_result abort_reload();
    const std::string& reload_report() const noexcept;

    minemod_scene_metrics metrics() const;
    std::string metrics_report() const;

    minemod_result describe_sharing(minemod_scene_sharing& sharing) const;
    minemod_result export_attachment(SharedAttachment attachment, std::int64_t& handle) const;
    minemod_result export_semaphores(ExportedSemaphores& semaphores) const;

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
    minemod_result clear_depth();
    minemod_result blur_frame(std::span<const std::byte> payload);
    minemod_result connect_section_faces(std::span<const std::byte> payload);
    minemod_result end_frame();

    SessionTargets create_targets(VkExtent2D extent);
    void upload_waiting_sections();
    void order_translucent_terrain();
    void select_reachable_sections();
    const renderer::FramesInFlight::Frame& recording();
    FrameSlot& recording_slot();
    std::uint64_t last_frame_using_removed_resources() const noexcept;
    std::optional<SampledTexture> sampled(minemod_handle texture);
    VkDescriptorSet bound_texture(minemod_handle texture, const SampledTexture& sampled);
    std::vector<renderer::ImageId> add_texture_writes(renderer::RenderGraph& graph, const FrameSlot& slot);
    std::optional<renderer::BlurredBackdrop> blurred_backdrop(FrameSlot& slot) const;
    std::size_t blurred_interface_batches() const noexcept;
    void submit_frame(const renderer::FramesInFlight::Frame& frame);
    std::vector<renderer::PassSample> pass_samples(std::span<const renderer::PassTiming> timings) const;
    std::uint32_t draws_in(std::string_view pass) const noexcept;
    FrameUsage usage_of(std::uint64_t frame, const FrameSlot& slot) const noexcept;
    SessionState state() const noexcept;

    std::shared_ptr<const SceneHost> host_;
    std::optional<renderer::WindowSurface> surface_;
    renderer::Device device_;
    std::optional<SessionSharing> sharing_;
    SceneLimits limits_;
    VkExtent2D extent_;
    renderer::RetirementQueue retirements_;
    std::optional<SessionTargets> targets_;
    renderer::SamplerCache samplers_;
    renderer::PipelineCache pipelines_;
    renderer::TerrainRenderer terrain_;
    renderer::BatchRenderer batches_;
    renderer::FrameBlurRenderer blur_;
    renderer::SceneRenderer scene_;
    ProgramReload reload_;
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
    SessionMetrics metrics_;
};

}
