#include "terrain/terrain_sections.hpp"

#include <utility>

namespace minemod::renderer {

bool TerrainSections::publish(
    const JobEpochs& epochs, const SectionJob& job, SectionBuffers buffers, std::uint64_t last_use_frame) {
    if (!epochs.is_current(job.section, job.stamp)) {
        return false;
    }

    const auto published = sections_.find(job.section);
    if (published == sections_.end()) {
        sections_.emplace(job.section, std::move(buffers));
        return true;
    }

    retirements_.retire(last_use_frame, std::exchange(published->second, std::move(buffers)));
    return true;
}

void TerrainSections::unload(const SectionPosition& section, std::uint64_t last_use_frame) {
    const auto published = sections_.find(section);
    if (published == sections_.end()) {
        return;
    }
    retirements_.retire(last_use_frame, std::move(published->second));
    sections_.erase(published);
}

void TerrainSections::unload_all(std::uint64_t last_use_frame) {
    for (auto& [section, buffers] : sections_) {
        retirements_.retire(last_use_frame, std::move(buffers));
    }
    sections_.clear();
}

const SectionBuffers* TerrainSections::find(const SectionPosition& section) const {
    const auto published = sections_.find(section);
    return published == sections_.end() ? nullptr : &published->second;
}

std::vector<SectionPosition> TerrainSections::positions() const {
    std::vector<SectionPosition> positions;
    positions.reserve(sections_.size());
    for (const auto& [section, buffers] : sections_) {
        positions.push_back(section);
    }
    return positions;
}

}
