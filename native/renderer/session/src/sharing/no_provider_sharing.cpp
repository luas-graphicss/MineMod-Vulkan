#include "sharing/provider_sharing.hpp"

#include "device/vulkan_error.hpp"

namespace minemod::scene {

namespace {

[[noreturn]] void refuse_sharing() {
    throw renderer::UnsupportedSystem("this build of the renderer shares no attachments with an OpenGL provider");
}

}

SharingExtensions sharing_extensions() {
    refuse_sharing();
}

std::unique_ptr<ProviderSharing> open_provider_sharing(const renderer::Device&) {
    refuse_sharing();
}

}
