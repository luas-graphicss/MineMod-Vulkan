#include <array>
#include <bit>
#include <cstdint>
#include <string_view>

#include <doctest.h>

#include "calls/call_table.hpp"
#include "calls/call_words.hpp"
#include "support/fake_provider_library.hpp"

namespace {

using namespace minemod::provider;
using minemod::provider::testing::last_call;
using minemod::provider::testing::open_fake_provider;

std::int64_t invoke(const ProviderLibrary& library, const char* name, std::span<const std::int64_t> words) {
    const std::optional<std::uint32_t> function = find_call(name);
    REQUIRE(function.has_value());
    const std::optional<std::int64_t> answer = invoke_call(*function, library.symbol(name), words);
    REQUIRE(answer.has_value());
    return *answer;
}

}

TEST_CASE("the call table is ordered by name so that every described function is found") {
    const std::span<const CallRecord> records = call_records();

    REQUIRE(records.size() > 700);
    for (std::size_t index = 1; index < records.size(); ++index) {
        CHECK(records[index - 1].name < records[index].name);
    }
    for (std::size_t index = 0; index < records.size(); ++index) {
        CHECK(find_call(records[index].name) == static_cast<std::uint32_t>(index));
    }
    CHECK_FALSE(find_call("glNoSuchFunction").has_value());
    CHECK_FALSE(find_call("").has_value());
}

TEST_CASE("functions that mods call have a thunk and functions only the layer calls do not") {
    const std::span<const CallRecord> records = call_records();

    CHECK(records[*find_call("glBindFramebuffer")].thunk != nullptr);
    CHECK(records[*find_call("glBindFramebufferEXT")].thunk != nullptr);
    CHECK(records[*find_call("glImportMemoryFdEXT")].thunk == nullptr);
}

TEST_CASE("an invocation carries single and double precision arguments bit for bit") {
    const ProviderLibrary library = open_fake_provider();

    const std::array<std::int64_t, 4> color{pack(0.25F), pack(0.5F), pack(-1.0F), pack(1.0F)};
    invoke(library, "glClearColor", color);
    CHECK(std::string_view(last_call(library).function) == "glClearColor");
    CHECK(unpack<float>(last_call(library).words[0]) == 0.25F);
    CHECK(unpack<float>(last_call(library).words[2]) == -1.0F);

    const std::array<std::int64_t, 3> vertex{pack(1.5), pack(-2.25), pack(1.0e300)};
    invoke(library, "glVertex3d", vertex);
    CHECK(unpack<double>(last_call(library).words[1]) == -2.25);
    CHECK(unpack<double>(last_call(library).words[2]) == 1.0e300);
}

TEST_CASE("an invocation with more arguments than registers keeps their order") {
    const ProviderLibrary library = open_fake_provider();
    const std::array<std::uint8_t, 4> pixels{};

    const std::array<std::int64_t, 9> image{0x0DE1, 1, 0x8058, 64, 32, 0, 0x1908, 0x1401, pack(pixels.data())};
    invoke(library, "glTexImage2D", image);

    const FakeCall& call = last_call(library);
    REQUIRE(call.word_count == image.size());
    for (std::size_t index = 0; index < image.size(); ++index) {
        CHECK(call.words[index] == image[index]);
    }
}

TEST_CASE("an invocation returns what the provider returned and lets it write through pointers") {
    const ProviderLibrary library = open_fake_provider();
    std::array<std::uint32_t, 2> names{};

    const std::array<std::int64_t, 2> generate{2, pack(names.data())};
    invoke(library, "glGenTextures", generate);
    CHECK(names[0] == 42);
    CHECK(names[1] == 43);

    const std::array<std::int64_t, 1> known{42};
    const std::array<std::int64_t, 1> unknown{7};
    CHECK(invoke(library, "glIsTexture", known) == 1);
    CHECK(invoke(library, "glIsTexture", unknown) == 0);
    CHECK(invoke(library, "glGetError", {}) == 0x0502);

    const std::array<std::int64_t, 2> map{0x8892, 0x88B9};
    CHECK(invoke(library, "glMapBuffer", map) != 0);
}

TEST_CASE("an invocation is refused when it names no function, no address or the wrong number of words") {
    const ProviderLibrary library = open_fake_provider();
    const std::uint32_t bind = *find_call("glBindTexture");
    const std::array<std::int64_t, 2> words{0x0DE1, 5};

    CHECK_FALSE(invoke_call(bind, nullptr, words).has_value());
    CHECK_FALSE(invoke_call(bind, library.symbol("glBindTexture"), std::span(words).first(1)).has_value());
    CHECK_FALSE(invoke_call(
        static_cast<std::uint32_t>(call_records().size()), library.symbol("glBindTexture"), words).has_value());
}
