#include <cstdint>

#include <doctest.h>

#include "library/loaded_image.hpp"
#include "support/fake_provider_library.hpp"

namespace {

using namespace minemod::provider;
using minemod::provider::testing::open_fake_provider;

void local_function() {}

std::uintptr_t address_of(void* pointer) {
    return reinterpret_cast<std::uintptr_t>(pointer);
}

}

TEST_CASE("the image covers the libraries loaded from the provider directory and nothing else") {
    const ProviderLibrary library = open_fake_provider();

    const std::vector<ImageSegment> image = segments_loaded_from(library.directory());

    REQUIRE_FALSE(image.empty());
    CHECK(contains_address(image, address_of(library.symbol("glGetError"))));
    CHECK(contains_address(image, address_of(library.symbol("eglGetProcAddress"))));
    CHECK_FALSE(contains_address(image, address_of(reinterpret_cast<void*>(&local_function))));
    CHECK_FALSE(contains_address(image, 0));
}

TEST_CASE("a directory nothing was loaded from has no image") {
    CHECK(segments_loaded_from("/nonexistent-provider-directory").empty());
}

TEST_CASE("a segment contains its first address and not the one past its last") {
    const ImageSegment segment{0x1000, 0x2000};

    CHECK(segment.contains(0x1000));
    CHECK(segment.contains(0x1FFF));
    CHECK_FALSE(segment.contains(0x2000));
    CHECK_FALSE(segment.contains(0x0FFF));
}
