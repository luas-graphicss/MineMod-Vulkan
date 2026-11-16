#pragma once

#include <cstddef>
#include <cstdint>
#include <optional>
#include <unordered_map>
#include <vector>

#include "math/vector.hpp"
#include "terrain/translucent_order.hpp"
#include "work/job_epochs.hpp"

namespace minemod::renderer {

class TranslucentSections {
public:
    void track(const SectionPosition& section, TranslucentQuads quads);
    void forget(const SectionPosition& section);
    void clear() noexcept;

    std::vector<SectionPosition> out_of_order(const WorldPosition& camera) const;
    std::vector<std::uint32_t> reorder(const SectionPosition& section, const WorldPosition& camera);

    std::size_t size() const noexcept {
        return sections_.size();
    }

private:
    struct CameraBlock {
        double x;
        double y;
        double z;

        friend bool operator==(const CameraBlock&, const CameraBlock&) = default;
    };

    struct Tracked {
        TranslucentQuads quads;
        std::optional<CameraBlock> ordered_for;
    };

    static CameraBlock block_containing(const WorldPosition& camera) noexcept;

    std::unordered_map<SectionPosition, Tracked, SectionPositionHash> sections_;
};

}
