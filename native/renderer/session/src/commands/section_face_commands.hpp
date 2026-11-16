#pragma once

#include <cstddef>
#include <cstdint>
#include <span>

#include "commands/scene_commands.hpp"
#include "visibility/section_faces.hpp"
#include "work/job_epochs.hpp"

namespace minemod::scene {

struct SectionFaces {
    renderer::SectionPosition section;
    std::uint32_t world;
    renderer::FaceConnections connections;
};

Decoded<SectionFaces> decode_section_faces(std::span<const std::byte> payload);

}
