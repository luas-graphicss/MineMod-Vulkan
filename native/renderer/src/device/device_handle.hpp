#pragma once

#include <string_view>
#include <utility>

#include <vulkan/vulkan.h>

#include "device/vulkan_error.hpp"

namespace minemod::renderer {

template <typename Handle>
class DeviceHandle {
public:
    using Destroy = void (VKAPI_PTR*)(VkDevice, Handle, const VkAllocationCallbacks*);

    DeviceHandle() noexcept = default;

    DeviceHandle(VkDevice device, Handle handle, Destroy destroy) noexcept
        : device_(device), handle_(handle), destroy_(destroy) {}

    DeviceHandle(DeviceHandle&& other) noexcept
        : device_(other.device_), handle_(std::exchange(other.handle_, Handle{})), destroy_(other.destroy_) {}

    DeviceHandle& operator=(DeviceHandle&& other) noexcept {
        if (this != &other) {
            release();
            device_ = other.device_;
            handle_ = std::exchange(other.handle_, Handle{});
            destroy_ = other.destroy_;
        }
        return *this;
    }

    DeviceHandle(const DeviceHandle&) = delete;
    DeviceHandle& operator=(const DeviceHandle&) = delete;

    ~DeviceHandle() {
        release();
    }

    Handle get() const noexcept {
        return handle_;
    }

private:
    void release() noexcept {
        if (handle_ != Handle{}) {
            destroy_(device_, handle_, nullptr);
            handle_ = Handle{};
        }
    }

    VkDevice device_ = VK_NULL_HANDLE;
    Handle handle_{};
    Destroy destroy_ = nullptr;
};

template <typename Handle, typename CreateInfo>
DeviceHandle<Handle> create_device_handle(
    VkDevice device,
    const CreateInfo& create_info,
    VkResult (VKAPI_PTR* create)(VkDevice, const CreateInfo*, const VkAllocationCallbacks*, Handle*),
    typename DeviceHandle<Handle>::Destroy destroy,
    std::string_view operation) {
    Handle handle{};
    require_success(create(device, &create_info, nullptr, &handle), operation);
    return DeviceHandle<Handle>(device, handle, destroy);
}

}
