#pragma once

#include <vulkan/vulkan.h>

#include "memory/exported_handle.hpp"

namespace minemod::interop {

class SemaphoreExport {
public:
    explicit SemaphoreExport(VkDevice device);

    ExportedHandle export_handle(VkSemaphore semaphore) const;

private:
    VkDevice device_;
    PFN_vkVoidFunction export_semaphore_;
};

}
