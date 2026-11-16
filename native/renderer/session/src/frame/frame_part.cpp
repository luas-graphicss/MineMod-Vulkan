#include "frame/frame_part.hpp"

#include <cstddef>

#include <vulkan/vulkan.h>

#include "minemod/abi/scene_commands.h"

namespace minemod::scene {

namespace {

constexpr renderer::ImageUse handed_back(const renderer::ImageUse& handed_over) noexcept {
    return renderer::ImageUse{
        handed_over.layout,
        VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT,
        VK_ACCESS_2_MEMORY_READ_BIT | VK_ACCESS_2_MEMORY_WRITE_BIT,
    };
}

}

PartTargets part_targets(const RecordedFrame& frame, PartEnding ending) noexcept {
    const bool is_resumed = frame.is_partly_drawn();
    const bool is_handed_over = ending == PartEnding::hands_over;
    return PartTargets{
        .color =
            {
                is_resumed ? handed_back(renderer::uses::color_attachment)
                           : renderer::discarded_after(renderer::uses::transfer_source),
                is_handed_over ? renderer::uses::color_attachment : renderer::uses::transfer_source,
            },
        .depth =
            {
                is_resumed ? handed_back(renderer::uses::depth_attachment)
                           : renderer::discarded_after(renderer::uses::depth_attachment),
                is_handed_over ? std::optional(renderer::uses::depth_attachment) : std::nullopt,
            },
    };
}

PartBatches undrawn_batches(const RecordedFrame& frame) noexcept {
    const std::span<const renderer::DrawBatch> interface = frame.undrawn(MINEMOD_BATCH_INTERFACE);
    const std::size_t blurred = frame.backdrop_blur
        ? frame.backdrop_blur->interface_batches - frame.progress.drawn_batches[MINEMOD_BATCH_INTERFACE]
        : 0;
    return PartBatches{
        frame.undrawn(MINEMOD_BATCH_SKY),
        frame.undrawn(MINEMOD_BATCH_SOLID),
        frame.undrawn(MINEMOD_BATCH_BLENDED),
        frame.undrawn(MINEMOD_BATCH_HAND),
        interface.first(blurred),
        interface.subspan(blurred),
    };
}

}
