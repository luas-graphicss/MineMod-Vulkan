#pragma once

#include <cstddef>
#include <unordered_map>
#include <vector>

#include "terrain/section_mesh.hpp"
#include "terrain/terrain_sections.hpp"
#include "work/job_epochs.hpp"

namespace minemod::renderer {

struct PendingSection {
    SectionJob job;
    SectionMesh mesh;
};

class PendingSections {
public:
    explicit PendingSections(std::size_t capacity);

    bool offer(PendingSection pending);
    std::size_t discard_stale(const JobEpochs& epochs);
    void remove(const SectionPosition& section);

    const PendingSection* find(const SectionPosition& section) const;
    std::vector<SectionPosition> positions() const;

    std::size_t size() const noexcept {
        return waiting_.size();
    }

    std::size_t capacity() const noexcept {
        return capacity_;
    }

private:
    std::size_t capacity_;
    std::unordered_map<SectionPosition, PendingSection, SectionPositionHash> waiting_;
};

}
