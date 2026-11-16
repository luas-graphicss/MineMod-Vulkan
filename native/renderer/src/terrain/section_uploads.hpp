#pragma once

#include <cstddef>
#include <cstdint>

#include <vulkan/vulkan.h>

#include "device/device.hpp"
#include "math/vector.hpp"
#include "terrain/pending_sections.hpp"
#include "terrain/terrain_sections.hpp"
#include "upload/upload_arena.hpp"
#include "work/job_epochs.hpp"

namespace minemod::renderer {

struct UploadPass {
    VkCommandBuffer commands;
    std::uint64_t frame;
    WorldPosition camera;
};

struct UploadedSections {
    std::size_t published;
    std::size_t discarded;
    std::size_t waiting;

    friend bool operator==(const UploadedSections&, const UploadedSections&) = default;
};

class SectionUploads {
public:
    SectionUploads(const Device& device, PendingSections& pending, TerrainSections& sections) noexcept
        : device_(device), pending_(pending), sections_(sections) {}

    UploadedSections record(const UploadPass& pass, UploadArena& arena, const JobEpochs& epochs);

private:
    bool upload(const UploadPass& pass, UploadArena& arena, const JobEpochs& epochs, const SectionPosition& section);

    const Device& device_;
    PendingSections& pending_;
    TerrainSections& sections_;
};

}
