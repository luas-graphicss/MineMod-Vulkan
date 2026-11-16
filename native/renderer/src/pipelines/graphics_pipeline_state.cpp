#include "pipelines/graphics_pipeline_state.hpp"

#include <functional>

namespace minemod::renderer {

namespace {

class StateDigest {
public:
    template <typename Field>
    void mix(const Field& field) noexcept {
        digest_ ^= std::hash<Field>{}(field) + golden_ratio + (digest_ << 6) + (digest_ >> 2);
    }

    std::size_t get() const noexcept {
        return digest_;
    }

private:
    static constexpr std::size_t golden_ratio = static_cast<std::size_t>(0x9e3779b97f4a7c15ULL);

    std::size_t digest_ = 0;
};

}

std::size_t GraphicsPipelineStateHash::operator()(const GraphicsPipelineState& state) const noexcept {
    StateDigest digest;
    digest.mix(state.vertex_shader);
    digest.mix(state.fragment_shader);
    digest.mix(state.layout);
    digest.mix(state.vertex_layout.stride);
    for (const VertexAttribute& attribute : state.vertex_layout.attributes) {
        digest.mix(attribute.location);
        digest.mix(attribute.format);
        digest.mix(attribute.offset);
    }
    digest.mix(state.topology);
    digest.mix(state.cull_mode);
    digest.mix(state.depth.test);
    digest.mix(state.depth.write);
    digest.mix(state.depth.compare);
    digest.mix(state.blending);
    digest.mix(state.color_format);
    digest.mix(state.depth_format);
    return digest.get();
}

}
