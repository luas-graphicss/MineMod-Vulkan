#include "minemod/scene/scene_session.h"

#include <cstdint>

#include "abi/scene_handles.hpp"
#include "sharing/provider_sharing.hpp"

static_assert(sizeof(minemod_shared_attachment) == 24);
static_assert(sizeof(minemod_scene_sharing) == 88);

namespace {

using minemod::scene::run_guarded;
using minemod::scene::SharedAttachment;

static_assert(static_cast<std::uint32_t>(SharedAttachment::color) == MINEMOD_SHARED_ATTACHMENT_COLOR);
static_assert(static_cast<std::uint32_t>(SharedAttachment::depth_stencil) == MINEMOD_SHARED_ATTACHMENT_DEPTH_STENCIL);
static_assert(minemod::scene::shared_attachment_count == MINEMOD_SHARED_ATTACHMENT_COUNT);

}

minemod_result minemod_scene_session_get_sharing(
    const minemod_scene_session* session, minemod_scene_sharing* sharing) noexcept {
    if (session == nullptr || sharing == nullptr) {
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }
    return run_guarded([&] { return session->core.describe_sharing(*sharing); });
}

minemod_result minemod_scene_session_export_attachment(
    const minemod_scene_session* session, uint32_t attachment, int64_t* handle) noexcept {
    if (session == nullptr || handle == nullptr || attachment >= MINEMOD_SHARED_ATTACHMENT_COUNT) {
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }
    return run_guarded([&] {
        return session->core.export_attachment(static_cast<SharedAttachment>(attachment), *handle);
    });
}

minemod_result minemod_scene_session_export_semaphores(
    const minemod_scene_session* session, int64_t* renderer_done, int64_t* provider_done) noexcept {
    if (session == nullptr || renderer_done == nullptr || provider_done == nullptr) {
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }
    return run_guarded([&] {
        minemod::scene::ExportedSemaphores semaphores{};
        const minemod_result exported = session->core.export_semaphores(semaphores);
        if (exported == MINEMOD_RESULT_OK) {
            *renderer_done = semaphores.renderer_done;
            *provider_done = semaphores.provider_done;
        }
        return exported;
    });
}
