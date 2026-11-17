#pragma once

#include <array>

#include <vulkan/vulkan.h>

#include "device/device_handle.hpp"
#include "handoff/ownership_transfer.hpp"
#include "memory/exported_handle.hpp"
#include "minemod/scene/scene_session.h"
#include "session/support/running_session.hpp"
#include "support/sharing_device.hpp"

namespace minemod::scene::test_support {

class ProviderStandIn {
public:
    ProviderStandIn(const minemod_shared_attachment& color, interop::ExportedHandle memory);

    DrawnFrame fill_after_reading(const std::array<float, 4>& fill);

private:
    interop::HandoffResources imported_color() const;

    interop::test_support::SharingDevice gpu_;
    VkExtent2D extent_;
    VkImageLayout handoff_layout_;
    renderer::DeviceHandle<VkDeviceMemory> memory_;
    renderer::DeviceHandle<VkImage> image_;
    interop::OwnershipTransfer acquire_;
    interop::OwnershipTransfer release_;
};

}
