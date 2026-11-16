#include <algorithm>
#include <string>
#include <vector>

#include <doctest.h>

#include "session/support/running_session.hpp"
#include "support/command_stream_corpus.hpp"

namespace {

using namespace minemod::scene::test_support;

constexpr std::uint32_t frame_edge = 64;
constexpr Rgb background{32, 32, 32};

std::vector<std::uint8_t> corpus_stream(const std::string& name) {
    const auto corpus = minemod::abi::testing::load_command_stream_corpus(MINEMOD_COMMAND_STREAM_CORPUS);
    const auto entry = std::ranges::find(corpus, name, &minemod::abi::testing::CorpusEntry::name);
    REQUIRE(entry != corpus.end());
    return entry->bytes;
}

}

TEST_CASE("the frame the Java writer reproduces byte for byte draws its terrain and its sprite") {
    RunningSession session(offscreen_config(frame_edge, frame_edge));

    REQUIRE(session.submit(corpus_stream("valid-scene-frame.stream")) == MINEMOD_RESULT_OK);

    const DrawnFrame frame = session.read_frame(frame_edge, frame_edge);
    CHECK_MESSAGE(frame.shows(40, 32, {255, 0, 0}), frame.describe(40, 32));
    CHECK_MESSAGE(frame.shows(4, 8, {255, 0, 0}), frame.describe(4, 8));
    CHECK_MESSAGE(frame.shows(12, 8, {0, 0, 255}), frame.describe(12, 8));
    CHECK_MESSAGE(frame.shows(8, 40, background), frame.describe(8, 40));
    const minemod_scene_counters counters = session.counters();
    CHECK(counters.frames_drawn == 1);
    CHECK(counters.sections_published == 1);
    CHECK(counters.live_sections == 0);
    CHECK(counters.live_textures == 2);
    CHECK(session.validation_text() == "");
}
