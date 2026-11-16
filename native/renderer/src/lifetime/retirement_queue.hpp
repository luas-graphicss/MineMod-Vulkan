#pragma once

#include <cstddef>
#include <cstdint>
#include <deque>
#include <memory>
#include <utility>

namespace minemod::renderer {

class RetirementQueue {
public:
    RetirementQueue() = default;
    RetirementQueue(const RetirementQueue&) = delete;
    RetirementQueue& operator=(const RetirementQueue&) = delete;
    ~RetirementQueue();

    template <typename Resource>
    void retire(std::uint64_t last_use_frame, Resource resource) {
        accept(last_use_frame, std::make_unique<Held<Resource>>(std::move(resource)));
    }

    std::size_t release_through(std::uint64_t completed_frame);

    std::size_t pending_count() const noexcept {
        return retired_.size();
    }

private:
    struct Retired {
        virtual ~Retired() = default;
    };

    template <typename Resource>
    struct Held final : Retired {
        explicit Held(Resource held) : resource(std::move(held)) {}

        Resource resource;
    };

    struct Entry {
        std::uint64_t last_use_frame;
        std::unique_ptr<Retired> resource;
    };

    void accept(std::uint64_t last_use_frame, std::unique_ptr<Retired> resource);

    std::deque<Entry> retired_;
};

}
