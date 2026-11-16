#pragma once

#include <memory>
#include <new>
#include <stdexcept>
#include <utility>

#include "device/vulkan_error.hpp"
#include "host/scene_host.hpp"
#include "minemod/abi/result.h"
#include "session/scene_session.hpp"

struct minemod_scene_host {
    std::shared_ptr<const minemod::scene::SceneHost> core;
};

struct minemod_scene_session {
    minemod_scene_session(
        std::shared_ptr<const minemod::scene::SceneHost> host, const minemod::scene::SessionSetup& setup)
        : core(std::move(host), setup) {}

    minemod::scene::SceneSession core;
};

namespace minemod::scene {

template <typename Operation>
minemod_result run_guarded(Operation&& operation) noexcept {
    try {
        return operation();
    } catch (const std::bad_alloc&) {
        return MINEMOD_RESULT_OUT_OF_MEMORY;
    } catch (const std::length_error&) {
        return MINEMOD_RESULT_LIMIT_EXCEEDED;
    } catch (const std::invalid_argument&) {
        return MINEMOD_RESULT_MALFORMED_STREAM;
    } catch (const renderer::UnsupportedSystem&) {
        return MINEMOD_RESULT_UNSUPPORTED_CAPABILITY;
    } catch (...) {
        return MINEMOD_RESULT_INTERNAL_ERROR;
    }
}

}
