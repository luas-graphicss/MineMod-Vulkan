#pragma once

#include <cstdint>
#include <optional>
#include <span>
#include <string>
#include <unordered_map>

#include <vulkan/vulkan.h>

#include "device/device.hpp"
#include "device/device_handle.hpp"
#include "draw/batch_renderer.hpp"
#include "lifetime/retirement_queue.hpp"
#include "minemod/abi/result.h"
#include "reload/resource_generations.hpp"

namespace minemod::scene {

struct SceneProgram {
    renderer::DeviceHandle<VkShaderModule> vertex;
    renderer::DeviceHandle<VkShaderModule> fragment;
};

using ScenePrograms = std::unordered_map<std::uint32_t, SceneProgram>;

class ProgramReload {
public:
    explicit ProgramReload(const renderer::Device& device) : device_(device), generations_(ScenePrograms{}) {}

    ProgramReload(const ProgramReload&) = delete;
    ProgramReload& operator=(const ProgramReload&) = delete;

    minemod_result begin();
    minemod_result stage(
        std::uint32_t program, std::span<const std::uint32_t> vertex, std::span<const std::uint32_t> fragment);
    minemod_result abort();
    minemod_result commit(
        std::uint64_t last_frame_of_current, renderer::BatchRenderer& batches, renderer::RetirementQueue& retirements);

    std::optional<renderer::BatchProgram> published(std::uint32_t program) const;

    std::uint64_t generation() const noexcept {
        return generations_.generation();
    }

    const std::string& report() const noexcept {
        return report_;
    }

private:
    void refuse(std::uint32_t program, const std::string& reason);

    const renderer::Device& device_;
    renderer::ResourceGenerations<ScenePrograms> generations_;
    std::optional<ScenePrograms> staging_;
    bool is_refused_ = false;
    std::string report_;
};

}
