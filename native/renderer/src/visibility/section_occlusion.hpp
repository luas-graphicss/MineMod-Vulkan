#pragma once

#include <cstddef>
#include <unordered_map>
#include <vector>

#include "visibility/section_faces.hpp"
#include "work/job_epochs.hpp"

namespace minemod::renderer {

class SectionOcclusion {
public:
    void describe(const SectionPosition& section, const FaceConnections& connections);
    void forget(const SectionPosition& section);
    void clear() noexcept;

    std::vector<SectionPosition> reachable_from(const SectionPosition& camera_section) const;

    std::size_t size() const noexcept {
        return sections_.size();
    }

private:
    std::unordered_map<SectionPosition, FaceConnections, SectionPositionHash> sections_;
};

}
