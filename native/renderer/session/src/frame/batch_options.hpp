#pragma once

#include <optional>

#include <vulkan/vulkan.h>

#include "draw/batch_renderer.hpp"
#include "draw/primitive_mode.hpp"
#include "draw/quad_order.hpp"
#include "draw/texture_transform.hpp"

namespace minemod::scene {

struct BatchOptions {
    renderer::QuadOrder order_for(renderer::PrimitiveMode mode) const noexcept {
        return mode == renderer::PrimitiveMode::quads ? quad_order : renderer::QuadOrder::submitted;
    }

    void apply_to(renderer::DrawBatch& batch) const {
        batch.clip = clip;
        batch.texture_transform = texture_transform;
    }

    std::optional<VkRect2D> clip;
    renderer::TextureTransform texture_transform;
    renderer::QuadOrder quad_order = renderer::QuadOrder::submitted;
};

}
