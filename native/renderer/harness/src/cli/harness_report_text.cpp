#include "cli/harness_report_text.hpp"

#include <ostream>

#include "frame/frame_targets.hpp"

namespace minemod::harness {

void write_harness_report(std::ostream& output, const HarnessReport& report) {
    const VkExtent2D extent = report.frame.extent();
    output << "device: " << report.device.name << " (" << renderer::device_type_name(report.device.type) << ")\n"
           << "driver: " << report.device.driver << '\n'
           << "vulkan: " << renderer::version_text(report.device.api_version) << '\n'
           << "frame: " << extent.width << 'x' << extent.height << ", color VkFormat " << renderer::frame_color_format
           << ", depth VkFormat " << renderer::frame_depth_format << '\n'
           << "presentation: " << report.presented_frames << " frame(s) on a headless surface, VkFormat "
           << report.presentation_format << '\n';

    if (report.validation == renderer::Validation::disabled) {
        output << "validation: not requested\n";
        return;
    }
    output << "validation: " << report.validation_messages.size() << " message(s)\n";
    for (const std::string& message : report.validation_messages) {
        output << "  " << message << '\n';
    }
}

}
