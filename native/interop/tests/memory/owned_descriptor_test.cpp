#include <fcntl.h>
#include <unistd.h>

#include <utility>

#include <doctest.h>

#include "memory/owned_descriptor.hpp"

namespace {

using namespace minemod::interop;

int open_descriptor() {
    return ::open("/dev/null", O_RDONLY | O_CLOEXEC);
}

bool is_open(int descriptor) {
    return ::fcntl(descriptor, F_GETFD) != -1;
}

}

TEST_CASE("an owned descriptor is closed when its owner goes away") {
    int descriptor = -1;
    {
        const OwnedDescriptor owner(open_descriptor());
        descriptor = owner.get();
        REQUIRE(is_open(descriptor));
    }

    CHECK_FALSE(is_open(descriptor));
}

TEST_CASE("releasing hands the descriptor over without closing it") {
    OwnedDescriptor owner(open_descriptor());

    const OwnedDescriptor importer(owner.release());

    CHECK_FALSE(owner.is_open());
    CHECK(is_open(importer.get()));
}

TEST_CASE("moving leaves exactly one owner") {
    OwnedDescriptor first(open_descriptor());
    const int descriptor = first.get();

    const OwnedDescriptor second(std::move(first));

    CHECK(second.get() == descriptor);
    CHECK(is_open(descriptor));
}

TEST_CASE("assigning closes the descriptor that was owned before") {
    OwnedDescriptor owner(open_descriptor());
    const int replaced = owner.get();
    OwnedDescriptor replacement(open_descriptor());
    const int kept = replacement.get();

    owner = std::move(replacement);

    CHECK_FALSE(is_open(replaced));
    CHECK(owner.get() == kept);
    CHECK(is_open(kept));
}

TEST_CASE("an owner without a descriptor closes nothing") {
    const OwnedDescriptor empty;

    CHECK_FALSE(empty.is_open());
}
