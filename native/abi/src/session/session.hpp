#pragma once

#include <cstddef>
#include <cstdint>
#include <span>

#include "minemod/abi/session.h"
#include "resources/buffer_registry.hpp"
#include "stream/command_stream_walker.hpp"

namespace minemod::abi {

class Session final : private CommandSink {
public:
    static minemod_result check_config(const minemod_session_config& config) noexcept;

    explicit Session(const minemod_session_config& config) noexcept;

    minemod_result submit(std::span<const std::byte> stream, minemod_stream_report& report);
    minemod_result read_buffer(
        minemod_handle buffer, std::uint64_t offset, std::span<std::byte> destination) const;
    minemod_result describe_buffer(minemod_handle buffer, minemod_buffer_description& description) const;
    minemod_session_counters counters() const noexcept;

private:
    minemod_result create_buffer(const CreateBufferCommand& command) override;
    minemod_result write_buffer(const WriteBufferCommand& command) override;
    minemod_result destroy_buffer(const DestroyBufferCommand& command) override;
    minemod_result scene_command(const SceneCommand& command) override;

    BufferRegistry buffers_;
    std::uint64_t submissions_accepted_ = 0;
    std::uint64_t submissions_rejected_ = 0;
    std::uint64_t submissions_aborted_ = 0;
    std::uint64_t commands_executed_ = 0;
    std::uint64_t executing_submission_ = 0;
};

}
