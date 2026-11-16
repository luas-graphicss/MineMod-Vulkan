#pragma once

#include <utility>

namespace minemod::interop {

class OwnedDescriptor {
public:
    OwnedDescriptor() noexcept = default;
    explicit OwnedDescriptor(int descriptor) noexcept : descriptor_(descriptor) {}
    OwnedDescriptor(OwnedDescriptor&& other) noexcept : descriptor_(other.release()) {}
    OwnedDescriptor& operator=(OwnedDescriptor&& other) noexcept;
    OwnedDescriptor(const OwnedDescriptor&) = delete;
    OwnedDescriptor& operator=(const OwnedDescriptor&) = delete;
    ~OwnedDescriptor();

    int get() const noexcept {
        return descriptor_;
    }

    bool is_open() const noexcept {
        return descriptor_ >= 0;
    }

    int release() noexcept {
        return std::exchange(descriptor_, closed);
    }

private:
    static constexpr int closed = -1;

    void close_owned() noexcept;

    int descriptor_ = closed;
};

}
