#pragma once

#include <array>

#include <jni.h>

#include "minemod/abi/command_stream.h"

namespace minemod::jni {

inline constexpr jlong no_command_field = -1;

constexpr minemod_stream_report rejected_stream_report() noexcept {
    return minemod_stream_report{.command_count = 0, .failed_command = MINEMOD_NO_COMMAND, .failed_offset = 0};
}

constexpr std::array<jlong, 3> stream_report_fields(const minemod_stream_report& report) noexcept {
    return {
        jlong{report.command_count},
        report.failed_command == MINEMOD_NO_COMMAND ? no_command_field : jlong{report.failed_command},
        static_cast<jlong>(report.failed_offset),
    };
}

}
