#pragma once

#include <cstddef>
#include <cstdint>
#include <unordered_map>
#include <vector>

#include "lifetime/retirement_queue.hpp"
#include "terrain/section_buffers.hpp"
#include "work/job_epochs.hpp"

namespace minemod::renderer {

struct SectionJob {
    SectionPosition section;
    JobStamp stamp;
};

class TerrainSections {
public:
    explicit TerrainSections(RetirementQueue& retirements) noexcept : retirements_(retirements) {}

    bool publish(const JobEpochs& epochs, const SectionJob& job, SectionBuffers buffers, std::uint64_t last_use_frame);
    void unload(const SectionPosition& section, std::uint64_t last_use_frame);
    void unload_all(std::uint64_t last_use_frame);

    const SectionBuffers* find(const SectionPosition& section) const;
    std::vector<SectionPosition> positions() const;

    std::size_t size() const noexcept {
        return sections_.size();
    }

private:
    RetirementQueue& retirements_;
    std::unordered_map<SectionPosition, SectionBuffers, SectionPositionHash> sections_;
};

}
