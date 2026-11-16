#include "lifetime/retirement_queue.hpp"

#include <stdexcept>

namespace minemod::renderer {

RetirementQueue::~RetirementQueue() {
    while (!retired_.empty()) {
        retired_.pop_front();
    }
}

void RetirementQueue::accept(std::uint64_t last_use_frame, std::unique_ptr<Retired> resource) {
    if (!retired_.empty() && last_use_frame < retired_.back().last_use_frame) {
        throw std::logic_error("a resource cannot be retired for a frame older than the last retired one");
    }
    retired_.push_back(Entry{last_use_frame, std::move(resource)});
}

std::size_t RetirementQueue::release_through(std::uint64_t completed_frame) {
    std::size_t released = 0;
    while (!retired_.empty() && retired_.front().last_use_frame <= completed_frame) {
        retired_.pop_front();
        ++released;
    }
    return released;
}

}
