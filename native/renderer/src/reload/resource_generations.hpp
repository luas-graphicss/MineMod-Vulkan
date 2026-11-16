#pragma once

#include <cstdint>
#include <optional>
#include <utility>

#include "lifetime/retirement_queue.hpp"

namespace minemod::renderer {

template <typename Resources>
class ResourceGenerations {
public:
    explicit ResourceGenerations(Resources initial) : current_(std::move(initial)) {}

    const Resources& current() const noexcept {
        return current_;
    }

    std::uint64_t generation() const noexcept {
        return generation_;
    }

    void stage(Resources prepared) {
        staged_ = std::move(prepared);
    }

    bool publish(std::uint64_t last_frame_of_current, RetirementQueue& retirements) {
        if (!staged_) {
            return false;
        }
        retirements.retire(last_frame_of_current, std::exchange(current_, std::move(*staged_)));
        staged_.reset();
        ++generation_;
        return true;
    }

private:
    Resources current_;
    std::optional<Resources> staged_;
    std::uint64_t generation_ = 0;
};

}
