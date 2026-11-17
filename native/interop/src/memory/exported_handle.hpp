#pragma once

#include <cstdint>
#include <utility>

namespace minemod::interop {

class ExportedHandle {
public:
    ExportedHandle() noexcept = default;
    explicit ExportedHandle(std::int64_t handle) noexcept : handle_(handle) {}
    ExportedHandle(ExportedHandle&& other) noexcept : handle_(other.release()) {}
    ExportedHandle& operator=(ExportedHandle&& other) noexcept;
    ExportedHandle(const ExportedHandle&) = delete;
    ExportedHandle& operator=(const ExportedHandle&) = delete;
    ~ExportedHandle();

    std::int64_t get() const noexcept {
        return handle_;
    }

    bool is_open() const noexcept {
        return handle_ != closed;
    }

    std::int64_t release() noexcept {
        return std::exchange(handle_, closed);
    }

private:
    static constexpr std::int64_t closed = -1;

    void close_owned() noexcept;

    std::int64_t handle_ = closed;
};

}
