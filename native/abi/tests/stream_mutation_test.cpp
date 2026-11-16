#include <doctest.h>

#include <cstdint>
#include <map>
#include <random>
#include <vector>

#include "minemod/abi/session.h"
#include "support/stream_builder.hpp"
#include "support/test_session.hpp"

using namespace minemod::abi::testing;

namespace {

constexpr std::uint32_t slot_count = 16;
constexpr int max_commands_per_stream = 12;
constexpr int max_buffer_bytes = 64;
constexpr int generated_streams = 300;
constexpr int mutations_per_stream = 40;

class SequenceGenerator {
public:
    explicit SequenceGenerator(std::uint64_t seed) : random_(seed), generations_(slot_count, 0) {}

    std::vector<std::uint8_t> next_stream() {
        StreamBuilder builder;
        const int commands = pick(1, max_commands_per_stream);
        for (int index = 0; index < commands; ++index) {
            append_command(builder);
        }
        return builder.build();
    }

    const std::map<minemod_handle, std::vector<std::uint8_t>>& expected_buffers() const {
        return expected_buffers_;
    }

private:
    enum class Action { create, write, destroy };

    int pick(int low, int high) {
        return std::uniform_int_distribution<int>(low, high)(random_);
    }

    Action pick_action() {
        if (expected_buffers_.empty()) {
            return Action::create;
        }
        if (expected_buffers_.size() == slot_count) {
            return pick(0, 1) == 0 ? Action::write : Action::destroy;
        }
        return static_cast<Action>(pick(0, 2));
    }

    void append_command(StreamBuilder& builder) {
        switch (pick_action()) {
            case Action::create:
                append_create(builder);
                break;
            case Action::write:
                append_write(builder);
                break;
            case Action::destroy:
                append_destroy(builder);
                break;
        }
    }

    void append_create(StreamBuilder& builder) {
        std::uint32_t slot = static_cast<std::uint32_t>(pick(0, static_cast<int>(slot_count) - 1));
        while (is_live(slot)) {
            slot = (slot + 1) % slot_count;
        }
        const minemod_handle handle = buffer_handle(slot, ++generations_[slot]);
        const auto size = static_cast<std::size_t>(pick(1, max_buffer_bytes));
        builder.create_buffer(handle, size, UINT32_C(1) << pick(0, 2));
        expected_buffers_[handle] = std::vector<std::uint8_t>(size, 0);
    }

    void append_write(StreamBuilder& builder) {
        auto& [handle, bytes] = *pick_live();
        const auto offset = static_cast<std::size_t>(pick(0, static_cast<int>(bytes.size()) - 1));
        const auto length = static_cast<std::size_t>(pick(1, static_cast<int>(bytes.size() - offset)));
        std::vector<std::uint8_t> data(length);
        for (std::uint8_t& byte : data) {
            byte = static_cast<std::uint8_t>(pick(0, 255));
        }
        std::copy(data.begin(), data.end(), bytes.begin() + static_cast<std::ptrdiff_t>(offset));
        builder.write_buffer(handle, offset, data);
    }

    void append_destroy(StreamBuilder& builder) {
        const auto live = pick_live();
        builder.destroy_buffer(live->first);
        expected_buffers_.erase(live);
    }

    std::map<minemod_handle, std::vector<std::uint8_t>>::iterator pick_live() {
        auto live = expected_buffers_.begin();
        std::advance(live, pick(0, static_cast<int>(expected_buffers_.size()) - 1));
        return live;
    }

    bool is_live(std::uint32_t slot) const {
        return expected_buffers_.contains(buffer_handle(slot, generations_[slot]));
    }

    std::mt19937_64 random_;
    std::vector<std::uint32_t> generations_;
    std::map<minemod_handle, std::vector<std::uint8_t>> expected_buffers_;
};

std::vector<std::uint8_t> mutate(std::vector<std::uint8_t> stream, std::mt19937_64& random) {
    const auto pick = [&](std::size_t low, std::size_t high) {
        return std::uniform_int_distribution<std::size_t>(low, high)(random);
    };
    const std::uint32_t interesting[] = {
        0, 1, 7, 8, 0x7FFFFFFF, 0xFFFFFFFF, static_cast<std::uint32_t>(stream.size()),
    };

    switch (pick(0, 2)) {
        case 0: {
            const std::size_t position = pick(0, stream.size() - 1);
            stream[position] = static_cast<std::uint8_t>(stream[position] ^ (1U << pick(0, 7)));
            break;
        }
        case 1: {
            const std::size_t field = pick(0, stream.size() / 4 - 1) * 4;
            store_u32(stream, field, interesting[pick(0, std::size(interesting) - 1)]);
            break;
        }
        default:
            stream.resize(pick(0, stream.size()));
            break;
    }
    return stream;
}

}

TEST_CASE("generated command sequences leave the buffers their model predicts") {
    SequenceGenerator generator(0x4D494E454D4F4401);
    TestSession session;

    for (int round = 0; round < generated_streams; ++round) {
        CAPTURE(round);
        REQUIRE(session.submit(generator.next_stream()) == MINEMOD_RESULT_OK);

        for (const auto& [handle, expected] : generator.expected_buffers()) {
            REQUIRE(session.read(handle, 0, expected.size()) == expected);
        }
        REQUIRE(session.counters().live_buffers == generator.expected_buffers().size());
    }
}

TEST_CASE("mutated streams end in a defined result without breaking the session budgets") {
    SequenceGenerator generator(0x4D494E454D4F4402);
    std::mt19937_64 random(0x4D494E454D4F4403);
    const auto config = default_session_config();

    for (int round = 0; round < generated_streams; ++round) {
        const auto stream = generator.next_stream();
        for (int mutation = 0; mutation < mutations_per_stream; ++mutation) {
            const auto mutated = mutate(stream, random);
            TestSession session(config);
            minemod_stream_report report{};

            const minemod_result result = session.submit(mutated, report);

            CAPTURE(round);
            CAPTURE(mutation);
            REQUIRE(result >= MINEMOD_RESULT_OK);
            REQUIRE(result < MINEMOD_RESULT_OUT_OF_MEMORY);
            if (result != MINEMOD_RESULT_OK) {
                REQUIRE(report.failed_offset <= mutated.size());
            }
            const auto counters = session.counters();
            REQUIRE(counters.live_buffers <= config.max_buffers);
            REQUIRE(counters.live_buffer_bytes <= config.max_buffer_bytes);
        }
    }
}
