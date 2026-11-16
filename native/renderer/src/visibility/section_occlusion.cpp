#include "visibility/section_occlusion.hpp"

#include <algorithm>
#include <bitset>
#include <cstdint>
#include <limits>
#include <optional>

namespace minemod::renderer {
namespace {

using FaceSet = std::bitset<section_face_count>;
using DescribedSections = std::unordered_map<SectionPosition, FaceConnections, SectionPositionHash>;

struct FaceStep {
    std::int32_t x;
    std::int32_t y;
    std::int32_t z;
};

constexpr SectionFace opposite(SectionFace face) noexcept {
    switch (face) {
    case SectionFace::west:
        return SectionFace::east;
    case SectionFace::east:
        return SectionFace::west;
    case SectionFace::down:
        return SectionFace::up;
    case SectionFace::up:
        return SectionFace::down;
    case SectionFace::north:
        return SectionFace::south;
    case SectionFace::south:
        break;
    }
    return SectionFace::north;
}

constexpr FaceStep step_through(SectionFace face) noexcept {
    switch (face) {
    case SectionFace::west:
        return {-1, 0, 0};
    case SectionFace::east:
        return {1, 0, 0};
    case SectionFace::down:
        return {0, -1, 0};
    case SectionFace::up:
        return {0, 1, 0};
    case SectionFace::north:
        return {0, 0, -1};
    case SectionFace::south:
        break;
    }
    return {0, 0, 1};
}

constexpr std::optional<std::int32_t> stepped(std::int32_t coordinate, std::int32_t step) noexcept {
    const std::int64_t moved = std::int64_t{coordinate} + step;
    if (moved < std::numeric_limits<std::int32_t>::min() || moved > std::numeric_limits<std::int32_t>::max()) {
        return std::nullopt;
    }
    return static_cast<std::int32_t>(moved);
}

constexpr std::optional<SectionPosition> neighbor(const SectionPosition& section, SectionFace face) noexcept {
    const FaceStep step = step_through(face);
    const std::optional<std::int32_t> x = stepped(section.x, step.x);
    const std::optional<std::int32_t> y = stepped(section.y, step.y);
    const std::optional<std::int32_t> z = stepped(section.z, step.z);
    if (!x || !y || !z) {
        return std::nullopt;
    }
    return SectionPosition{*x, *y, *z};
}

struct Reached {
    SectionPosition section;
    FaceSet traveled;
    FaceSet arrived_by;
};

bool can_leave(const Reached& reached, const FaceConnections& connections, SectionFace exit) noexcept {
    if (reached.traveled.test(face_index(opposite(exit)))) {
        return false;
    }
    if (reached.arrived_by.none()) {
        return true;
    }
    return std::ranges::any_of(section_faces, [&](SectionFace direction) {
        return reached.arrived_by.test(face_index(direction)) && connections.connects(opposite(direction), exit);
    });
}

class Flood {
public:
    Flood(const DescribedSections& sections, const SectionPosition& start) : sections_(sections) {
        reached_.push_back({start, {}, {}});
        reached_at_.emplace(start, 0);
        for (std::size_t next = 0; next < reached_.size(); ++next) {
            spread_from(reached_[next]);
        }
    }

    std::vector<SectionPosition> sections() const {
        std::vector<SectionPosition> positions;
        positions.reserve(reached_.size());
        for (const Reached& reached : reached_) {
            positions.push_back(reached.section);
        }
        return positions;
    }

private:
    void spread_from(const Reached from) {
        const FaceConnections& connections = sections_.at(from.section);
        for (const SectionFace exit : section_faces) {
            if (can_leave(from, connections, exit)) {
                arrive_beyond(from, exit);
            }
        }
    }

    void arrive_beyond(const Reached& from, SectionFace exit) {
        const std::optional<SectionPosition> beyond = neighbor(from.section, exit);
        if (!beyond || !sections_.contains(*beyond)) {
            return;
        }

        const auto [known, is_new] = reached_at_.try_emplace(*beyond, reached_.size());
        if (!is_new) {
            reached_[known->second].arrived_by.set(face_index(exit));
            return;
        }
        FaceSet traveled = from.traveled;
        traveled.set(face_index(exit));
        reached_.push_back({*beyond, traveled, FaceSet{}.set(face_index(exit))});
    }

    const DescribedSections& sections_;
    std::vector<Reached> reached_;
    std::unordered_map<SectionPosition, std::size_t, SectionPositionHash> reached_at_;
};

}

void SectionOcclusion::describe(const SectionPosition& section, const FaceConnections& connections) {
    sections_.insert_or_assign(section, connections);
}

void SectionOcclusion::forget(const SectionPosition& section) {
    sections_.erase(section);
}

void SectionOcclusion::clear() noexcept {
    sections_.clear();
}

std::vector<SectionPosition> SectionOcclusion::reachable_from(const SectionPosition& camera_section) const {
    if (sections_.contains(camera_section)) {
        return Flood(sections_, camera_section).sections();
    }

    std::vector<SectionPosition> described;
    described.reserve(sections_.size());
    for (const auto& [section, connections] : sections_) {
        described.push_back(section);
    }
    return described;
}

}
