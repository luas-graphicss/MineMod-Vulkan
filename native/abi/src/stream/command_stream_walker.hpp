#pragma once

#include <cstddef>
#include <cstdint>
#include <functional>
#include <span>

#include "minemod/abi/command_stream.h"
#include "minemod/abi/handle.h"
#include "minemod/abi/result.h"

namespace minemod::abi {

struct CreateBufferCommand {
    minemod_handle handle;
    std::uint64_t size_bytes;
    std::uint32_t usage;
};

struct WriteBufferCommand {
    minemod_handle handle;
    std::uint64_t offset;
    std::span<const std::byte> data;
};

struct DestroyBufferCommand {
    minemod_handle handle;
};

struct SceneCommand {
    std::uint16_t opcode;
    std::span<const std::byte> payload;
};

class CommandSink {
public:
    virtual minemod_result create_buffer(const CreateBufferCommand& command) = 0;
    virtual minemod_result write_buffer(const WriteBufferCommand& command) = 0;
    virtual minemod_result destroy_buffer(const DestroyBufferCommand& command) = 0;
    virtual minemod_result scene_command(const SceneCommand& command) = 0;

protected:
    ~CommandSink() = default;
};

minemod_result walk_command_stream(
    std::span<const std::byte> stream, CommandSink& sink, minemod_stream_report& report);

minemod_result validate_command_stream(std::span<const std::byte> stream, minemod_stream_report& report);

using RecordVisitor = std::function<minemod_result(const SceneCommand& record)>;

minemod_result visit_command_stream(
    std::span<const std::byte> stream, const RecordVisitor& visitor, minemod_stream_report& report);

}
