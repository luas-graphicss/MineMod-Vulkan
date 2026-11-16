#include "frame/viewport.hpp"

namespace minemod::renderer {

void set_upright_viewport(VkCommandBuffer commands, VkExtent2D extent) {
    const auto width = static_cast<float>(extent.width);
    const auto height = static_cast<float>(extent.height);
    const VkViewport viewport{0.0f, height, width, -height, 0.0f, 1.0f};
    const VkRect2D scissor{{0, 0}, extent};
    vkCmdSetViewport(commands, 0, 1, &viewport);
    vkCmdSetScissor(commands, 0, 1, &scissor);
}

}
