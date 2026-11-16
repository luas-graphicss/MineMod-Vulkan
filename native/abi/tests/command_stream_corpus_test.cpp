#include <doctest.h>

#include <string>

#include "minemod/abi/command_stream.h"
#include "support/command_stream_corpus.hpp"
#include "support/stream_builder.hpp"

using namespace minemod::abi::testing;

TEST_CASE("every corpus stream validates to its recorded result") {
    const auto corpus = load_command_stream_corpus(MINEMOD_COMMAND_STREAM_CORPUS);
    REQUIRE(corpus.size() >= 16);

    for (const CorpusEntry& entry : corpus) {
        CAPTURE(entry.name);
        minemod_stream_report report{};
        const minemod_result result =
            minemod_validate_command_stream(entry.bytes.data(), entry.bytes.size(), &report);
        CHECK(std::string(minemod_result_name(result)) == entry.expected_result);
    }
}

TEST_CASE("the builder reproduces the buffer lifecycle stream of the corpus byte for byte") {
    const std::uint8_t data[] = {1, 2, 3, 4, 5};
    const auto built = StreamBuilder()
                           .create_buffer(buffer_handle(0), 16, MINEMOD_BUFFER_USAGE_VERTEX)
                           .write_buffer(buffer_handle(0), 4, data)
                           .destroy_buffer(buffer_handle(0))
                           .build();

    const auto corpus = load_command_stream_corpus(MINEMOD_COMMAND_STREAM_CORPUS);
    bool found = false;
    for (const CorpusEntry& entry : corpus) {
        if (entry.name == "valid-buffer-lifecycle.stream") {
            found = true;
            CHECK(entry.bytes == built);
        }
    }
    CHECK(found);
}
