#pragma once

#include <utility>
#include <vector>

namespace minemod::renderer::test_support {

class TrackedResource {
public:
    TrackedResource(std::vector<int>& released, int id) : released_(&released), id_(id) {}

    TrackedResource(TrackedResource&& other) noexcept
        : released_(std::exchange(other.released_, nullptr)), id_(other.id_) {}

    TrackedResource& operator=(TrackedResource&& other) noexcept {
        if (this != &other) {
            release();
            released_ = std::exchange(other.released_, nullptr);
            id_ = other.id_;
        }
        return *this;
    }

    ~TrackedResource() {
        release();
    }

    int id() const noexcept {
        return id_;
    }

private:
    void release() noexcept {
        if (released_ != nullptr) {
            released_->push_back(id_);
            released_ = nullptr;
        }
    }

    std::vector<int>* released_;
    int id_;
};

}
