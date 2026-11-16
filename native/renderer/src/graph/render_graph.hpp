#pragma once

#include <cstdint>
#include <functional>
#include <optional>
#include <span>
#include <string>
#include <variant>
#include <vector>

#include <vulkan/vulkan.h>

#include "graph/image_use.hpp"
#include "resources/image.hpp"

namespace minemod::renderer {

class GraphImages;

struct ImageId {
    std::uint32_t index;

    friend bool operator==(const ImageId&, const ImageId&) = default;
};

struct PassId {
    std::uint32_t index;

    friend bool operator==(const PassId&, const PassId&) = default;
};

struct ImportedImage {
    VkImage handle;
    VkImageView view;
    VkExtent2D extent;
    VkImageAspectFlags aspect;
    ImageUse entry;
    std::optional<ImageUse> exit;
};

struct ImageAccess {
    ImageId image;
    ImageUse use;
};

using RecordPass = std::function<void(VkCommandBuffer, const GraphImages&)>;

struct GraphImage {
    std::string name;
    std::variant<ImageDescription, ImportedImage> source;
};

struct GraphPass {
    std::string name;
    std::vector<ImageAccess> accesses;
    std::vector<PassId> dependencies;
    RecordPass record;
};

class RenderGraph {
public:
    ImageId create_image(std::string name, const ImageDescription& description);
    ImageId import_image(std::string name, const ImportedImage& image);

    PassId add_pass(std::string name, std::vector<ImageAccess> accesses, RecordPass record);
    void add_dependency(PassId pass, PassId dependency);

    std::span<const GraphImage> images() const noexcept {
        return images_;
    }

    std::span<const GraphPass> passes() const noexcept {
        return passes_;
    }

private:
    const GraphPass& declared_pass(PassId pass) const;

    std::vector<GraphImage> images_;
    std::vector<GraphPass> passes_;
};

}
