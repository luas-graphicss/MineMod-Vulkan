#pragma once

#include <vulkan/vulkan.h>

namespace minemod::renderer {

void set_upright_viewport(VkCommandBuffer commands, VkExtent2D extent);

}
