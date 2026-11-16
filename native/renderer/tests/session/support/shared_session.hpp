#pragma once

#include <cstdint>

#include <vulkan/vulkan.h>

#include "memory/owned_descriptor.hpp"
#include "minemod/scene/scene_session.h"
#include "session/support/running_session.hpp"

namespace minemod::scene::test_support {

minemod_scene_session_config shared_config(VkExtent2D extent);
minemod_scene_sharing sharing_of(const RunningSession& session);
interop::OwnedDescriptor exported_memory(const RunningSession& session, std::uint32_t attachment);

}
