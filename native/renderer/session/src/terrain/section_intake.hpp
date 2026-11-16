#pragma once

#include <cstddef>
#include <unordered_map>

#include <vulkan/vulkan.h>

#include "commands/scene_commands.hpp"
#include "device/device.hpp"
#include "frame/frame_slot.hpp"
#include "minemod/abi/result.h"
#include "terrain/pending_sections.hpp"
#include "terrain/section_uploads.hpp"
#include "terrain/terrain_sections.hpp"
#include "terrain/translucent_order.hpp"
#include "terrain/translucent_sections.hpp"
#include "upload/upload_arena.hpp"
#include "work/job_epochs.hpp"

namespace minemod::scene {

class SectionIntake {
public:
    SectionIntake(const renderer::Device& device, renderer::TerrainSections& sections, const SceneLimits& limits);

    SectionIntake(const SectionIntake&) = delete;
    SectionIntake& operator=(const SectionIntake&) = delete;

    minemod_result accept(renderer::JobEpochs& epochs, const SectionPublication& publication);
    std::size_t forget(const renderer::JobEpochs& epochs, const renderer::SectionPosition& section);
    std::size_t forget_all(const renderer::JobEpochs& epochs);

    renderer::UploadedSections upload(
        const renderer::UploadPass& pass, renderer::UploadArena& arena, const renderer::JobEpochs& epochs);
    std::size_t reorder_translucent(const renderer::UploadPass& pass, renderer::UploadArena& arena);

    bool is_waiting() const noexcept {
        return pending_.size() != 0;
    }

private:
    struct WaitingQuads {
        renderer::JobStamp stamp;
        renderer::TranslucentQuads quads;
    };

    bool has_room_for(const renderer::SectionPosition& section) const;
    std::size_t discard_stale(const renderer::JobEpochs& epochs);
    void track_published(const renderer::JobEpochs& epochs);

    renderer::TerrainSections& sections_;
    std::size_t max_sections_;
    VkDeviceSize upload_bytes_per_frame_;
    renderer::PendingSections pending_;
    renderer::SectionUploads uploads_;
    renderer::TranslucentSections translucent_;
    std::unordered_map<renderer::SectionPosition, WaitingQuads, renderer::SectionPositionHash> waiting_quads_;
};

}
