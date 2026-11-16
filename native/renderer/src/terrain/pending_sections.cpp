#include "terrain/pending_sections.hpp"

#include <stdexcept>
#include <utility>

namespace minemod::renderer {

PendingSections::PendingSections(std::size_t capacity) : capacity_(capacity) {
    if (capacity == 0) {
        throw std::invalid_argument("pending sections need room for at least one section");
    }
}

bool PendingSections::offer(PendingSection pending) {
    require_drawable(pending.mesh);

    const auto waiting = waiting_.find(pending.job.section);
    if (waiting != waiting_.end()) {
        if (pending.job.stamp.revision > waiting->second.job.stamp.revision) {
            waiting->second = std::move(pending);
        }
        return true;
    }
    if (waiting_.size() == capacity_) {
        return false;
    }
    const SectionPosition section = pending.job.section;
    waiting_.emplace(section, std::move(pending));
    return true;
}

std::size_t PendingSections::discard_stale(const JobEpochs& epochs) {
    return std::erase_if(waiting_, [&epochs](const auto& waiting) {
        return !epochs.is_current(waiting.first, waiting.second.job.stamp);
    });
}

void PendingSections::remove(const SectionPosition& section) {
    waiting_.erase(section);
}

const PendingSection* PendingSections::find(const SectionPosition& section) const {
    const auto waiting = waiting_.find(section);
    return waiting == waiting_.end() ? nullptr : &waiting->second;
}

std::vector<SectionPosition> PendingSections::positions() const {
    std::vector<SectionPosition> positions;
    positions.reserve(waiting_.size());
    for (const auto& [section, pending] : waiting_) {
        positions.push_back(section);
    }
    return positions;
}

}
