#include "work/job_epochs.hpp"

#include <functional>

namespace minemod::renderer {

namespace {

std::uint64_t coordinate_bits(std::int32_t coordinate) {
    return std::uint64_t{static_cast<std::uint32_t>(coordinate)};
}

}

std::size_t SectionPositionHash::operator()(const SectionPosition& position) const noexcept {
    const std::uint64_t horizontal = (coordinate_bits(position.x) << 32) | coordinate_bits(position.z);
    const std::uint64_t vertical = coordinate_bits(position.y) * 0x9e3779b97f4a7c15ULL;
    return std::hash<std::uint64_t>{}(horizontal ^ vertical);
}

JobStamp JobEpochs::schedule(const SectionPosition& section) {
    const std::uint64_t revision = ++last_revision_;
    revisions_.insert_or_assign(section, revision);
    return JobStamp{world_, resources_, revision};
}

void JobEpochs::unload(const SectionPosition& section) {
    revisions_.erase(section);
}

void JobEpochs::change_world() {
    ++world_;
    revisions_.clear();
}

void JobEpochs::reload_resources() noexcept {
    ++resources_;
}

bool JobEpochs::is_current(const SectionPosition& section, const JobStamp& stamp) const {
    if (stamp.world != world_ || stamp.resources != resources_) {
        return false;
    }
    const auto revision = revisions_.find(section);
    return revision != revisions_.end() && revision->second == stamp.revision;
}

}
