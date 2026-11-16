#include "session/support/shared_session.hpp"

#include <stdexcept>
#include <string>

namespace minemod::scene::test_support {

namespace {

void require_ok(minemod_result result, const char* operation) {
    if (result != MINEMOD_RESULT_OK) {
        throw std::runtime_error(std::string(operation) + " failed with " + minemod_result_name(result));
    }
}

}

minemod_scene_session_config shared_config(VkExtent2D extent) {
    minemod_scene_session_config config = offscreen_config(extent.width, extent.height);
    config.flags = MINEMOD_SCENE_SESSION_SHARED_ATTACHMENTS;
    return config;
}

minemod_scene_sharing sharing_of(const RunningSession& session) {
    minemod_scene_sharing sharing{};
    require_ok(minemod_scene_session_get_sharing(session.get(), &sharing), "describing the shared attachments");
    return sharing;
}

interop::OwnedDescriptor exported_memory(const RunningSession& session, std::uint32_t attachment) {
    std::int64_t handle = -1;
    require_ok(
        minemod_scene_session_export_attachment(session.get(), attachment, &handle), "exporting a shared attachment");
    return interop::OwnedDescriptor(static_cast<int>(handle));
}

}
