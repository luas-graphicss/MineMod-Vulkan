#pragma once

#include <cstdint>

#include <vulkan/vulkan.h>

#include "memory/exported_handle.hpp"
#include "minemod/scene/scene_session.h"
#include "session/support/running_session.hpp"

namespace minemod::scene::test_support {

minemod_scene_session_config shared_config(VkExtent2D extent);
minemod_scene_sharing sharing_of(const RunningSession& session);
interop::ExportedHandle exported_memory(const RunningSession& session, std::uint32_t attachment);

}
