#include "terrain/section_intake.hpp"

#include <algorithm>
#include <cstdint>
#include <span>
#include <utility>
#include <vector>

#include "terrain/section_buffers.hpp"
#include "terrain/section_mesh.hpp"
#include "terrain/terrain_layer.hpp"
#include "terrain/terrain_vertex.hpp"

namespace minemod::scene {

namespace {

constexpr std::size_t max_reordered_sections_per_frame = 15;
constexpr VkDeviceSize order_alignment = 1;

std::size_t waiting_capacity(const SceneLimits& limits) noexcept {
    return std::max<std::size_t>(limits.max_sections, 1);
}

VkDeviceSize staged_bytes(const renderer::SectionMesh& mesh) noexcept {
    return VkDeviceSize{mesh.vertices.size()} * sizeof(renderer::TerrainVertex)
        + VkDeviceSize{mesh.indices.size()} * sizeof(std::uint32_t);
}

VkDeviceSize order_bytes(const renderer::SectionBuffers& buffers) noexcept {
    return VkDeviceSize{buffers.range(renderer::TerrainLayer::translucent).index_count} * sizeof(std::uint32_t);
}

void wait_for_earlier_uses_of_indices(VkCommandBuffer commands) {
    const VkMemoryBarrier2 barrier{
        .sType = VK_STRUCTURE_TYPE_MEMORY_BARRIER_2,
        .srcStageMask = VK_PIPELINE_STAGE_2_VERTEX_INPUT_BIT | VK_PIPELINE_STAGE_2_COPY_BIT,
        .srcAccessMask = VK_ACCESS_2_INDEX_READ_BIT | VK_ACCESS_2_TRANSFER_WRITE_BIT,
        .dstStageMask = VK_PIPELINE_STAGE_2_COPY_BIT,
        .dstAccessMask = VK_ACCESS_2_TRANSFER_WRITE_BIT,
    };
    const VkDependencyInfo dependency{
        .sType = VK_STRUCTURE_TYPE_DEPENDENCY_INFO,
        .memoryBarrierCount = 1,
        .pMemoryBarriers = &barrier,
    };
    vkCmdPipelineBarrier2(commands, &dependency);
}

}

SectionIntake::SectionIntake(
    const renderer::Device& device, renderer::TerrainSections& sections, const SceneLimits& limits)
    : sections_(sections),
      max_sections_(limits.max_sections),
      upload_bytes_per_frame_(limits.upload_bytes_per_frame),
      pending_(waiting_capacity(limits)),
      uploads_(device, pending_, sections) {}

minemod_result SectionIntake::accept(renderer::JobEpochs& epochs, const SectionPublication& publication) {
    renderer::TranslucentQuads quads = renderer::translucent_quads(publication.mesh);
    if (!has_room_for(publication.section) || staged_bytes(publication.mesh) > upload_bytes_per_frame_) {
        return MINEMOD_RESULT_LIMIT_EXCEEDED;
    }

    const renderer::SectionJob job{publication.section, epochs.schedule(publication.section)};
    if (!pending_.offer(renderer::PendingSection{job, publication.mesh})) {
        return MINEMOD_RESULT_LIMIT_EXCEEDED;
    }
    waiting_quads_.insert_or_assign(publication.section, WaitingQuads{job.stamp, std::move(quads)});
    return MINEMOD_RESULT_OK;
}

std::size_t SectionIntake::forget(const renderer::JobEpochs& epochs, const renderer::SectionPosition& section) {
    translucent_.forget(section);
    return discard_stale(epochs);
}

std::size_t SectionIntake::forget_all(const renderer::JobEpochs& epochs) {
    translucent_.clear();
    return discard_stale(epochs);
}

renderer::UploadedSections SectionIntake::upload(
    const renderer::UploadPass& pass, renderer::UploadArena& arena, const renderer::JobEpochs& epochs) {
    const renderer::UploadedSections uploaded = uploads_.record(pass, arena, epochs);
    track_published(epochs);
    return uploaded;
}

std::size_t SectionIntake::reorder_translucent(const renderer::UploadPass& pass, renderer::UploadArena& arena) {
    std::size_t reordered = 0;
    for (const renderer::SectionPosition& section : translucent_.out_of_order(pass.camera)) {
        const renderer::SectionBuffers& buffers = *sections_.find(section);
        const bool has_room = order_bytes(buffers) <= upload_bytes_per_frame_ - arena.used();
        if (reordered == max_reordered_sections_per_frame || !has_room) {
            break;
        }
        if (reordered == 0) {
            wait_for_earlier_uses_of_indices(pass.commands);
        }

        const std::vector<std::uint32_t> order = translucent_.reorder(section, pass.camera);
        const renderer::ArenaRegion staged = arena.stage(std::as_bytes(std::span(order)), order_alignment).value();
        buffers.record_translucent_order(pass.commands, arena.buffer(), staged);
        ++reordered;
    }
    return reordered;
}

bool SectionIntake::has_room_for(const renderer::SectionPosition& section) const {
    if (sections_.find(section) != nullptr || pending_.find(section) != nullptr) {
        return true;
    }
    if (sections_.size() + pending_.size() < max_sections_) {
        return true;
    }

    const std::vector<renderer::SectionPosition> waiting = pending_.positions();
    const auto unpublished = std::ranges::count_if(waiting, [this](const renderer::SectionPosition& position) {
        return sections_.find(position) == nullptr;
    });
    return sections_.size() + static_cast<std::size_t>(unpublished) < max_sections_;
}

std::size_t SectionIntake::discard_stale(const renderer::JobEpochs& epochs) {
    const std::size_t discarded = pending_.discard_stale(epochs);
    std::erase_if(waiting_quads_, [this](const auto& waiting) { return pending_.find(waiting.first) == nullptr; });
    return discarded;
}

void SectionIntake::track_published(const renderer::JobEpochs& epochs) {
    std::erase_if(waiting_quads_, [this, &epochs](auto& waiting) {
        if (pending_.find(waiting.first) != nullptr) {
            return false;
        }
        if (epochs.is_current(waiting.first, waiting.second.stamp)) {
            translucent_.track(waiting.first, std::move(waiting.second.quads));
        }
        return true;
    });
}

}
