#include <array>
#include <cstddef>
#include <cstdint>
#include <span>
#include <string>
#include <string_view>
#include <vector>

#include <doctest.h>

#include "calls/call_table.hpp"
#include "calls/call_words.hpp"
#include "library/driver_settings.hpp"
#include "library/loaded_image.hpp"
#include "session/provider_session.hpp"
#include "support/staged_bundle.hpp"

namespace {

using namespace minemod::provider;
using minemod::provider::testing::read_staged_bundle;
using minemod::provider::testing::StagedBundle;

constexpr std::int64_t gl_renderer = 0x1F01;
constexpr std::int64_t gl_extensions = 0x1F03;
constexpr std::int64_t gl_major_version = 0x821B;
constexpr std::int64_t gl_minor_version = 0x821C;
constexpr std::int64_t gl_num_extensions = 0x821D;

#if defined(_WIN32)
constexpr std::string_view memory_handle_extension = "GL_EXT_memory_object_win32";
#else
constexpr std::string_view memory_handle_extension = "GL_EXT_memory_object_fd";
#endif

StagedBundle staged_bundle_with_its_settings() {
    StagedBundle bundle = read_staged_bundle(MINEMOD_PROVIDER_BUNDLE_DIRECTORY);
    apply_driver_settings(bundle.driver_settings);
    return bundle;
}

std::int64_t answer_of(ProviderSession& session, const char* function, std::span<const std::int64_t> words) {
    return session.invoke(*find_call(function), words);
}

std::int32_t integer_of(ProviderSession& session, std::int64_t parameter) {
    std::int32_t value = 0;
    const std::array<std::int64_t, 2> words{parameter, pack(&value)};
    answer_of(session, "glGetIntegerv", words);
    return value;
}

std::string renderer_of(ProviderSession& session) {
    const std::array<std::int64_t, 1> words{gl_renderer};
    return unpack<const char*>(answer_of(session, "glGetString", words));
}

bool offers(ProviderSession& session, std::string_view extension) {
    const std::int32_t count = integer_of(session, gl_num_extensions);
    for (std::int32_t index = 0; index < count; ++index) {
        const std::array<std::int64_t, 2> words{gl_extensions, index};
        if (unpack<const char*>(answer_of(session, "glGetStringi", words)) == extension) {
            return true;
        }
    }
    return false;
}

}

TEST_CASE("a staged bundle serves OpenGL 4.5 through Zink in the core and the compatibility profile") {
    const StagedBundle bundle = staged_bundle_with_its_settings();
    ProviderSession session(bundle.directory, bundle.dependencies, bundle.entry);

    for (const ContextProfile profile : {ContextProfile::core, ContextProfile::compatibility}) {
        const EglHandle context = session.display().create_context({profile, 4, 5}, nullptr);
        session.display().make_current(context);
        const std::string renderer = renderer_of(session);
        CAPTURE(renderer);

        CHECK(renderer.starts_with("zink"));
        CHECK(integer_of(session, gl_major_version) == 4);
        CHECK(integer_of(session, gl_minor_version) >= 5);
        CHECK(offers(session, "GL_EXT_memory_object"));
        CHECK(offers(session, memory_handle_extension));

        session.display().release_current();
        session.display().destroy_context(context);
    }
}

TEST_CASE("every function a staged bundle offers lies inside the image of that bundle") {
    const StagedBundle bundle = staged_bundle_with_its_settings();
    ProviderSession session(bundle.directory, bundle.dependencies, bundle.entry);
    const std::vector<ImageSegment> image(session.image().begin(), session.image().end());

    std::size_t offered = 0;
    for (const CallRecord& record : call_records()) {
        const std::string name(record.name);
        void* address = session.function_address(name.c_str());
        if (address == nullptr) {
            continue;
        }
        ++offered;
        CAPTURE(name);
        CHECK(contains_address(image, reinterpret_cast<std::uintptr_t>(address)));
    }

    CHECK(offered > 0);
}
