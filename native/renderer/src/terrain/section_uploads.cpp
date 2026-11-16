#include "terrain/section_uploads.hpp"

#include <optional>
#include <stdexcept>
#include <utility>

#include "terrain/section_buffers.hpp"
#include "terrain/section_selection.hpp"
#include "terrain/terrain_layer.hpp"

namespace minemod::renderer {

UploadedSections SectionUploads::record(const UploadPass& pass, UploadArena& arena, const JobEpochs& epochs) {
    const std::size_t discarded = pending_.discard_stale(epochs);
    std::size_t published = 0;
    for (const SectionPosition& section :
        ordered_sections(pending_.positions(), pass.camera, DrawDirection::front_to_back)) {
        if (!upload(pass, arena, epochs, section)) {
            break;
        }
        ++published;
    }
    return UploadedSections{published, discarded, pending_.size()};
}

bool SectionUploads::upload(
    const UploadPass& pass, UploadArena& arena, const JobEpochs& epochs, const SectionPosition& section) {
    const PendingSection& pending = *pending_.find(section);
    const bool arena_was_empty = arena.used() == 0;
    const std::optional<StagedSection> staged = stage_section(arena, pending.mesh);
    if (!staged && arena_was_empty) {
        pending_.remove(section);
        throw std::length_error("a section mesh does not fit an empty upload arena");
    }
    if (!staged) {
        return false;
    }

    SectionBuffers buffers(device_, pending.mesh);
    buffers.record_upload(pass.commands, arena.buffer(), *staged);
    sections_.publish(epochs, pending.job, std::move(buffers), pass.frame);
    pending_.remove(section);
    return true;
}

}
