#pragma once

#include <cstddef>
#include <cstdint>
#include <span>

#include <vulkan/vulkan.h>

#include "frame/batch_options.hpp"
#include "minemod/abi/result.h"

namespace minemod::scene {

minemod_result change_batch_options(
    BatchOptions& options, VkExtent2D frame, std::uint16_t opcode, std::span<const std::byte> payload);

}
