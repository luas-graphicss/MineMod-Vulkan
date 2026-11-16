#include "session/scene_session.hpp"

#include <algorithm>
#include <cstring>
#include <utility>

#include "commands/batch_option_commands.hpp"
#include "commands/scene_commands.hpp"
#include "device/physical_device.hpp"
#include "frame/frame_readback.hpp"
#include "graph/graph_images.hpp"
#include "graph/image_use.hpp"
#include "graph/planned_frame.hpp"
#include "minemod/abi/command_stream.h"
#include "minemod/abi/scene_commands.h"
#include "scene/scene_frame.hpp"
#include "terrain/section_buffers.hpp"
#include "textures/mip_chain.hpp"
#include "upload/upload_arena.hpp"

namespace minemod::scene {

namespace {

constexpr std::array presentation_extensions{VK_KHR_SWAPCHAIN_EXTENSION_NAME};
constexpr VkExtent2D white_extent{1, 1};
constexpr std::array white_texel{std::byte{0xFF}, std::byte{0xFF}, std::byte{0xFF}, std::byte{0xFF}};
constexpr renderer::SamplerDescription white_sampling{
    VK_FILTER_NEAREST,
    VK_FILTER_NEAREST,
    VK_SAMPLER_MIPMAP_MODE_NEAREST,
    VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE,
    1,
};

std::span<const char* const> device_extensions(VkSurfaceKHR window_surface) noexcept {
    if (window_surface == VK_NULL_HANDLE) {
        return {};
    }
    return presentation_extensions;
}

std::optional<renderer::WindowSurface> adopt(const SceneHost& host, VkSurfaceKHR window_surface) {
    if (window_surface == VK_NULL_HANDLE) {
        return std::nullopt;
    }
    return std::optional<renderer::WindowSurface>(std::in_place, host.instance(), window_surface);
}

renderer::Device create_device(const SceneHost& host, VkSurfaceKHR window_surface) {
    const std::span<const char* const> extensions = device_extensions(window_surface);
    return renderer::Device(
        renderer::select_device(
            host.instance(),
            renderer::DeviceRequirements{.extensions = extensions, .presentation_surface = window_surface}),
        extensions);
}

renderer::Texture create_white_texture(const renderer::Device& device) {
    renderer::Texture white(device, white_extent, 1);
    const renderer::MipChain chain = renderer::build_mip_chain(white_extent, white_texel, 1);
    renderer::UploadArena staging(device, chain.rgba.size());
    const renderer::ArenaRegion staged = staging.stage(chain.rgba, MINEMOD_TEXEL_BYTES).value();
    device.submit([&](VkCommandBuffer commands) { white.record_upload(commands, staging.buffer(), staged, chain); });
    return white;
}

bool has_area(VkExtent2D extent) noexcept {
    return extent.width != 0 && extent.height != 0;
}

bool lies_within(const TextureWrite& write, const SceneTexture& texture) noexcept {
    if (write.level >= texture.mip_levels) {
        return false;
    }
    const VkExtent2D extent = texture.texture.image().extent();
    const std::uint32_t width = std::max(extent.width >> write.level, 1u);
    const std::uint32_t height = std::max(extent.height >> write.level, 1u);
    const renderer::SpriteRegion& region = write.region;
    const bool is_inside = region.width != 0 && region.height != 0 && region.x < width && region.y < height
        && region.width <= width - region.x && region.height <= height - region.y;
    return is_inside && write.texels.size() == std::uint64_t{region.width} * region.height * MINEMOD_TEXEL_BYTES;
}

renderer::ImportedImage imported(
    const renderer::Image& image,
    VkImageAspectFlags aspect,
    const renderer::ImageUse& entry,
    const std::optional<renderer::ImageUse>& exit) {
    return renderer::ImportedImage{image.get(), image.view(), image.extent(), aspect, entry, exit};
}

void make_uploads_readable_by_draws(VkCommandBuffer commands) {
    const VkMemoryBarrier2 barrier{
        .sType = VK_STRUCTURE_TYPE_MEMORY_BARRIER_2,
        .srcStageMask = VK_PIPELINE_STAGE_2_COPY_BIT,
        .srcAccessMask = VK_ACCESS_2_TRANSFER_WRITE_BIT,
        .dstStageMask = VK_PIPELINE_STAGE_2_VERTEX_INPUT_BIT,
        .dstAccessMask = VK_ACCESS_2_VERTEX_ATTRIBUTE_READ_BIT | VK_ACCESS_2_INDEX_READ_BIT,
    };
    const VkDependencyInfo dependency{
        .sType = VK_STRUCTURE_TYPE_DEPENDENCY_INFO,
        .memoryBarrierCount = 1,
        .pMemoryBarriers = &barrier,
    };
    vkCmdPipelineBarrier2(commands, &dependency);
}

}

SceneSession::SceneSession(std::shared_ptr<const SceneHost> host, const SessionSetup& setup)
    : host_(std::move(host)),
      surface_(adopt(*host_, setup.window_surface)),
      device_(create_device(*host_, setup.window_surface)),
      limits_(setup.limits),
      extent_(setup.extent),
      targets_(std::in_place, device_, setup.extent),
      samplers_(device_),
      pipelines_(device_),
      terrain_(device_, pipelines_),
      batches_(device_, pipelines_),
      scene_(terrain_, batches_),
      sections_(retirements_),
      textures_(device_, samplers_, setup.limits.max_textures),
      white_(create_white_texture(device_)),
      white_sampler_(samplers_.sampler_for(white_sampling)),
      slots_{FrameSlot(device_, setup.limits), FrameSlot(device_, setup.limits)},
      frames_(device_, frames_in_flight) {
    if (surface_) {
        presenter_.emplace(device_, surface_->get(), setup.extent, frames_in_flight);
    }
}

SceneSession::~SceneSession() {
    vkDeviceWaitIdle(device_.get());
}

minemod_result SceneSession::execute(std::uint16_t opcode, std::span<const std::byte> payload) {
    switch (opcode) {
        case MINEMOD_OPCODE_DEFINE_TEXTURE:
            return define_texture(payload);
        case MINEMOD_OPCODE_WRITE_TEXTURE:
            return write_texture(payload);
        case MINEMOD_OPCODE_DESTROY_TEXTURE:
            return destroy_texture(payload);
        case MINEMOD_OPCODE_PUBLISH_SECTION:
            return publish_section(payload);
        case MINEMOD_OPCODE_UNLOAD_SECTION:
            return unload_section(payload);
        case MINEMOD_OPCODE_CHANGE_WORLD:
            return change_world();
        case MINEMOD_OPCODE_BEGIN_FRAME:
            return begin_frame(payload);
        case MINEMOD_OPCODE_SET_VIEW:
            return set_view(payload);
        case MINEMOD_OPCODE_SET_TERRAIN:
            return set_terrain(payload);
        case MINEMOD_OPCODE_DRAW_BATCH:
            return draw_batch(payload);
        case MINEMOD_OPCODE_END_FRAME:
            return end_frame();
        case MINEMOD_OPCODE_SET_CLIP:
        case MINEMOD_OPCODE_CLEAR_CLIP:
        case MINEMOD_OPCODE_SET_TEXTURE_TRANSFORM:
        case MINEMOD_OPCODE_SET_QUAD_ORDER:
            return set_batch_option(opcode, payload);
        default:
            return MINEMOD_RESULT_UNSUPPORTED_CAPABILITY;
    }
}

minemod_result SceneSession::resize(VkExtent2D extent) {
    if (frame_.is_open) {
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }
    if (presenter_) {
        presenter_->resize(extent);
    }
    const bool is_unchanged = extent.width == extent_.width && extent.height == extent_.height;
    if (!has_area(extent) || is_unchanged) {
        return MINEMOD_RESULT_OK;
    }
    retirements_.retire(last_frame_using_removed_resources(), std::move(*targets_));
    targets_.emplace(device_, extent);
    extent_ = extent;
    has_drawn_frame_ = false;
    return MINEMOD_RESULT_OK;
}

void SceneSession::set_present_pacing(renderer::PresentPacing pacing) noexcept {
    if (presenter_) {
        presenter_->set_pacing(pacing);
    }
}

std::uint32_t SceneSession::presentation_state() const noexcept {
    if (!presenter_) {
        return MINEMOD_PRESENTATION_OFFSCREEN;
    }
    switch (presenter_->state()) {
        case renderer::FrameState::awaiting_swapchain:
            return MINEMOD_PRESENTATION_AWAITING_SWAPCHAIN;
        case renderer::FrameState::running:
            return MINEMOD_PRESENTATION_RUNNING;
        case renderer::FrameState::suspended:
            return MINEMOD_PRESENTATION_SUSPENDED;
        case renderer::FrameState::device_lost:
            return MINEMOD_PRESENTATION_DEVICE_LOST;
        case renderer::FrameState::stopped:
            break;
    }
    return MINEMOD_PRESENTATION_STOPPED;
}

minemod_result SceneSession::read_frame(std::span<std::byte> rgba) {
    if (!has_drawn_frame_) {
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }
    const renderer::FramePixels pixels = renderer::read_back_color(device_, targets_->color());
    if (rgba.size() != pixels.rgba().size()) {
        return MINEMOD_RESULT_LENGTH_OUT_OF_BOUNDS;
    }
    std::memcpy(rgba.data(), pixels.rgba().data(), rgba.size());
    return MINEMOD_RESULT_OK;
}

minemod_scene_counters SceneSession::counters() const noexcept {
    minemod_scene_counters counters = counters_;
    counters.live_sections = sections_.size();
    counters.live_textures = textures_.size();
    counters.live_texture_bytes = textures_.texel_bytes();
    return counters;
}

minemod_result SceneSession::define_texture(std::span<const std::byte> payload) {
    const Decoded<TextureDefinition> definition = decode_texture_definition(payload);
    if (definition.result() != MINEMOD_RESULT_OK) {
        return definition.result();
    }
    const minemod_result defined = textures_.define(definition.command());
    if (defined == MINEMOD_RESULT_OK) {
        frame_.unread_textures.push_back(definition.command().texture);
    }
    return defined;
}

minemod_result SceneSession::write_texture(std::span<const std::byte> payload) {
    const Decoded<TextureWrite> decoded = decode_texture_write(payload);
    if (decoded.result() != MINEMOD_RESULT_OK) {
        return decoded.result();
    }
    const TextureWrite& write = decoded.command();
    const SceneTexture* texture = textures_.find(write.texture);
    if (texture == nullptr) {
        return MINEMOD_RESULT_INVALID_HANDLE;
    }
    if (!lies_within(write, *texture)) {
        return MINEMOD_RESULT_LENGTH_OUT_OF_BOUNDS;
    }
    const std::optional<renderer::ArenaRegion> staged =
        recording_slot().uploads.stage(write.texels, MINEMOD_TEXEL_BYTES);
    if (!staged) {
        return MINEMOD_RESULT_LIMIT_EXCEEDED;
    }
    frame_.texture_writes.push_back({write.texture, write.level, write.region, *staged});
    return MINEMOD_RESULT_OK;
}

minemod_result SceneSession::destroy_texture(std::span<const std::byte> payload) {
    const minemod_handle texture = read_texture_handle(payload);
    std::optional<SceneTexture> removed = textures_.remove(texture);
    if (!removed) {
        return MINEMOD_RESULT_INVALID_HANDLE;
    }
    std::erase(frame_.unread_textures, texture);
    std::erase_if(frame_.texture_writes, [texture](const StagedTextureWrite& write) {
        return write.texture == texture;
    });
    frame_.bound_textures.erase(texture);
    retirements_.retire(last_frame_using_removed_resources(), std::move(*removed));
    return MINEMOD_RESULT_OK;
}

minemod_result SceneSession::publish_section(std::span<const std::byte> payload) {
    const Decoded<SectionPublication> decoded = decode_section_publication(payload);
    if (decoded.result() != MINEMOD_RESULT_OK) {
        return decoded.result();
    }
    const SectionPublication& publication = decoded.command();
    if (publication.world != world_) {
        ++counters_.sections_dropped;
        return MINEMOD_RESULT_OK;
    }
    const bool is_new_section = sections_.find(publication.section) == nullptr;
    if (is_new_section && sections_.size() >= limits_.max_sections) {
        return MINEMOD_RESULT_LIMIT_EXCEEDED;
    }
    renderer::SectionBuffers buffers(device_, publication.mesh);
    const renderer::FramesInFlight::Frame& frame = recording();
    FrameSlot& slot = recording_slot();
    const std::optional<renderer::StagedSection> staged = renderer::stage_section(slot.uploads, publication.mesh);
    if (!staged) {
        return MINEMOD_RESULT_LIMIT_EXCEEDED;
    }
    buffers.record_upload(frame.commands, slot.uploads.buffer(), *staged);
    const renderer::SectionJob job{publication.section, epochs_.schedule(publication.section)};
    sections_.publish(epochs_, job, std::move(buffers), frame.number);
    frame_.uploaded_sections = true;
    ++counters_.sections_published;
    return MINEMOD_RESULT_OK;
}

minemod_result SceneSession::unload_section(std::span<const std::byte> payload) {
    const Decoded<renderer::SectionPosition> section = decode_section_position(payload);
    if (section.result() != MINEMOD_RESULT_OK) {
        return section.result();
    }
    epochs_.unload(section.command());
    sections_.unload(section.command(), last_frame_using_removed_resources());
    return MINEMOD_RESULT_OK;
}

minemod_result SceneSession::change_world() {
    epochs_.change_world();
    sections_.unload_all(last_frame_using_removed_resources());
    ++world_;
    return MINEMOD_RESULT_OK;
}

minemod_result SceneSession::begin_frame(std::span<const std::byte> payload) {
    const Decoded<FrameStart> start = decode_frame_start(payload);
    if (start.result() != MINEMOD_RESULT_OK) {
        return start.result();
    }
    if (frame_.is_open) {
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }
    recording();
    frame_.is_open = true;
    frame_.draws_world = start.command().draws_world;
    frame_.clear_color = start.command().clear_color;
    return MINEMOD_RESULT_OK;
}

minemod_result SceneSession::set_view(std::span<const std::byte> payload) {
    const Decoded<ViewDefinition> decoded = decode_view_definition(payload);
    if (decoded.result() != MINEMOD_RESULT_OK) {
        return decoded.result();
    }
    const ViewDefinition& definition = decoded.command();
    if (!frame_.is_open || frame_.views[definition.view] != VK_NULL_HANDLE) {
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }
    const std::optional<SampledTexture> lightmap = sampled(definition.lightmap);
    const std::optional<SampledTexture> overlay = sampled(definition.overlay);
    if (!lightmap || !overlay) {
        return MINEMOD_RESULT_INVALID_HANDLE;
    }
    FrameSlot& slot = recording_slot();
    const renderer::ViewConstants& constants = slot.views[definition.view];
    constants.write(definition.scene);
    frame_.views[definition.view] = batches_.bind_view(
        slot.descriptors,
        constants,
        renderer::ViewTextures{lightmap->view, lightmap->sampler, overlay->view, overlay->sampler});
    return MINEMOD_RESULT_OK;
}

minemod_result SceneSession::set_terrain(std::span<const std::byte> payload) {
    const Decoded<TerrainDefinition> decoded = decode_terrain_definition(payload);
    if (decoded.result() != MINEMOD_RESULT_OK) {
        return decoded.result();
    }
    const TerrainDefinition& definition = decoded.command();
    if (!frame_.is_open || !frame_.draws_world || frame_.terrain) {
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }
    const SceneTexture* atlas = textures_.find(definition.atlas);
    const SceneTexture* lightmap = textures_.find(definition.lightmap);
    if (atlas == nullptr || lightmap == nullptr) {
        return MINEMOD_RESULT_INVALID_HANDLE;
    }
    const VkDescriptorSet textures = terrain_.bind_textures(
        recording_slot().descriptors,
        renderer::TerrainTextures{
            atlas->texture.image().view(),
            atlas->sampler,
            lightmap->texture.image().view(),
            lightmap->sampler,
        });
    frame_.terrain =
        renderer::TerrainFrame{extent_, definition.view_projection, definition.camera, definition.fog, textures};
    return MINEMOD_RESULT_OK;
}

minemod_result SceneSession::draw_batch(std::span<const std::byte> payload) {
    const Decoded<BatchSubmission> decoded = decode_batch_submission(payload);
    if (decoded.result() != MINEMOD_RESULT_OK) {
        return decoded.result();
    }
    const BatchSubmission& batch = decoded.command();
    const bool belongs_to_world = batch.slot != MINEMOD_BATCH_INTERFACE;
    const bool has_stage = frame_.is_open && (frame_.draws_world || !belongs_to_world);
    if (!has_stage || frame_.views[batch.view] == VK_NULL_HANDLE) {
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }
    const std::size_t stride = batch.format.stride();
    if (batch.vertices.size() % stride != 0 || batch.vertices.size() / stride > renderer::max_batch_vertices) {
        return MINEMOD_RESULT_LENGTH_OUT_OF_BOUNDS;
    }
    const std::optional<SampledTexture> texture = sampled(batch.texture);
    if (!texture) {
        return MINEMOD_RESULT_INVALID_HANDLE;
    }
    const std::optional<renderer::BatchGeometry> staged =
        recording_slot().stager.stage(
            {batch.format, batch.mode, batch.vertices}, frame_.batch_options.order_for(batch.mode));
    if (!staged) {
        return MINEMOD_RESULT_LIMIT_EXCEEDED;
    }
    frame_.batches[batch.slot].push_back(renderer::DrawBatch{
        batch.format,
        *staged,
        batch.material,
        bound_texture(batch.texture, *texture),
        batch.model_view,
        batch.color_modulator,
        batch.line_width,
        frame_.views[batch.view],
    });
    frame_.batch_options.apply_to(frame_.batches[batch.slot].back());
    return MINEMOD_RESULT_OK;
}

minemod_result SceneSession::set_batch_option(std::uint16_t opcode, std::span<const std::byte> payload) {
    if (!frame_.is_open) {
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }
    return change_batch_options(frame_.batch_options, extent_, opcode, payload);
}

minemod_result SceneSession::end_frame() {
    if (!frame_.is_open || (frame_.draws_world && !frame_.terrain)) {
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }
    const renderer::FramesInFlight::Frame frame = recording();
    FrameSlot& slot = recording_slot();
    if (frame_.uploaded_sections) {
        make_uploads_readable_by_draws(frame.commands);
    }

    renderer::RenderGraph graph;
    const renderer::ImageId color = graph.import_image(
        "frame color",
        imported(
            targets_->color(),
            VK_IMAGE_ASPECT_COLOR_BIT,
            renderer::discarded_after(renderer::uses::transfer_source),
            renderer::uses::transfer_source));
    const renderer::ImageId depth = graph.import_image(
        "frame depth",
        imported(
            targets_->depth(),
            VK_IMAGE_ASPECT_DEPTH_BIT,
            renderer::discarded_after(renderer::uses::depth_attachment),
            std::nullopt));
    std::vector<renderer::ImageId> written_textures = add_texture_writes(graph, slot);

    std::optional<renderer::WorldStage> world;
    if (frame_.draws_world) {
        world = renderer::WorldStage{
            VK_NULL_HANDLE,
            *frame_.terrain,
            frame_.batches[MINEMOD_BATCH_SKY],
            frame_.batches[MINEMOD_BATCH_SOLID],
            frame_.batches[MINEMOD_BATCH_BLENDED],
        };
    }
    const renderer::SceneFrame scene{
        extent_,
        frame_.clear_color,
        world,
        {VK_NULL_HANDLE, frame_.batches[MINEMOD_BATCH_HAND]},
        {VK_NULL_HANDLE, frame_.batches[MINEMOD_BATCH_INTERFACE]},
    };
    scene_.add_passes(graph, renderer::SceneTargets{color, depth, std::move(written_textures)}, scene, sections_);

    slot.planned = std::make_unique<renderer::PlannedFrame>(device_, std::move(graph));
    slot.planned->record(frame.commands);
    submit_frame(frame);

    for (const std::vector<renderer::DrawBatch>& batches : frame_.batches) {
        counters_.batches_drawn += batches.size();
    }
    counters_.peak_upload_bytes = std::max<std::uint64_t>(counters_.peak_upload_bytes, slot.uploads.used());
    counters_.peak_geometry_bytes = std::max<std::uint64_t>(counters_.peak_geometry_bytes, slot.geometry.used());
    ++counters_.frames_drawn;
    has_drawn_frame_ = true;
    frame_.clear();
    return MINEMOD_RESULT_OK;
}

const renderer::FramesInFlight::Frame& SceneSession::recording() {
    if (!recording_) {
        recording_ = frames_.begin();
        if (const std::optional<std::uint64_t> completed = frames_.completed_frame()) {
            retirements_.release_through(*completed);
        }
        slots_[recording_->number % frames_in_flight].reuse();
    }
    return *recording_;
}

FrameSlot& SceneSession::recording_slot() {
    return slots_[recording().number % frames_in_flight];
}

std::uint64_t SceneSession::last_frame_using_removed_resources() const noexcept {
    return recording_ ? recording_->number : last_submitted_frame_.value_or(0);
}

std::optional<SceneSession::SampledTexture> SceneSession::sampled(minemod_handle texture) {
    if (texture == MINEMOD_HANDLE_NULL) {
        return SampledTexture{white_.image().view(), white_sampler_};
    }
    const SceneTexture* found = textures_.find(texture);
    if (found == nullptr) {
        return std::nullopt;
    }
    return SampledTexture{found->texture.image().view(), found->sampler};
}

VkDescriptorSet SceneSession::bound_texture(minemod_handle texture, const SampledTexture& sampled) {
    const auto bound = frame_.bound_textures.find(texture);
    if (bound != frame_.bound_textures.end()) {
        return bound->second;
    }
    const VkDescriptorSet set =
        batches_.bind_texture(recording_slot().descriptors, sampled.view, sampled.sampler);
    frame_.bound_textures.emplace(texture, set);
    return set;
}

std::vector<renderer::ImageId> SceneSession::add_texture_writes(renderer::RenderGraph& graph, const FrameSlot& slot) {
    struct Copy {
        const renderer::Texture* texture;
        StagedTextureWrite write;
    };
    std::vector<minemod_handle> touched = frame_.unread_textures;
    for (const StagedTextureWrite& write : frame_.texture_writes) {
        touched.push_back(write.texture);
    }
    std::ranges::sort(touched);
    touched.erase(std::unique(touched.begin(), touched.end()), touched.end());
    if (touched.empty()) {
        return {};
    }

    std::vector<renderer::ImageId> images;
    std::vector<renderer::ImageAccess> accesses;
    for (const minemod_handle handle : touched) {
        SceneTexture& texture = *textures_.find(handle);
        const renderer::ImageUse entry =
            texture.is_readable ? renderer::uses::sampled_by_shaders : renderer::uses::untouched;
        const renderer::ImageId image = graph.import_image(
            "scene texture",
            imported(texture.texture.image(), VK_IMAGE_ASPECT_COLOR_BIT, entry, renderer::uses::sampled_by_shaders));
        texture.is_readable = true;
        images.push_back(image);
        accesses.push_back({image, renderer::uses::transfer_destination});
    }

    std::vector<Copy> copies;
    copies.reserve(frame_.texture_writes.size());
    for (const StagedTextureWrite& write : frame_.texture_writes) {
        copies.push_back({&textures_.find(write.texture)->texture, write});
    }
    const VkBuffer staging = slot.uploads.buffer();
    graph.add_pass(
        "write textures",
        std::move(accesses),
        [staging, copies = std::move(copies)](VkCommandBuffer commands, const renderer::GraphImages&) {
            for (const Copy& copy : copies) {
                copy.texture->record_region_update(
                    commands, staging, copy.write.staged, copy.write.level, copy.write.region);
            }
        });
    return images;
}

void SceneSession::submit_frame(const renderer::FramesInFlight::Frame& frame) {
    const std::size_t slot = frame.number % frames_in_flight;
    const std::optional<renderer::PresentationTarget> target =
        presenter_ ? presenter_->acquire(slot) : std::nullopt;
    if (target) {
        presenter_->record_copy(frame.commands, targets_->color(), *target);
    }
    frames_.submit(target ? target->semaphores : renderer::SubmitSemaphores{});
    last_submitted_frame_ = frame.number;
    recording_.reset();
    if (target) {
        presenter_->present(*target);
        ++counters_.frames_presented;
    }
}

}
