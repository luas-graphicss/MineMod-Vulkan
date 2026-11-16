#pragma once

#include <cstddef>
#include <cstdint>
#include <unordered_map>

namespace minemod::renderer {

struct SectionPosition {
    std::int32_t x;
    std::int32_t y;
    std::int32_t z;

    friend bool operator==(const SectionPosition&, const SectionPosition&) = default;
};

struct SectionPositionHash {
    std::size_t operator()(const SectionPosition& position) const noexcept;
};

struct JobStamp {
    std::uint64_t world;
    std::uint64_t resources;
    std::uint64_t revision;
};

class JobEpochs {
public:
    JobStamp schedule(const SectionPosition& section);
    void unload(const SectionPosition& section);
    void change_world();
    void reload_resources() noexcept;

    bool is_current(const SectionPosition& section, const JobStamp& stamp) const;

private:
    std::uint64_t world_ = 0;
    std::uint64_t resources_ = 0;
    std::uint64_t last_revision_ = 0;
    std::unordered_map<SectionPosition, std::uint64_t, SectionPositionHash> revisions_;
};

}
