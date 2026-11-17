#include <cstdint>
#include <utility>

#include <doctest.h>

#include "memory/exported_handle.hpp"
#include "support/open_handles.hpp"

namespace {

using namespace minemod::interop;
using minemod::interop::test_support::is_handle_open;
using minemod::interop::test_support::open_handle;

}

TEST_CASE("an exported handle is closed when its owner goes away") {
    std::int64_t handle = -1;
    {
        const ExportedHandle owner(open_handle());
        handle = owner.get();
        REQUIRE(is_handle_open(handle));
    }

    CHECK_FALSE(is_handle_open(handle));
}

TEST_CASE("releasing hands the handle over without closing it") {
    ExportedHandle owner(open_handle());

    const ExportedHandle importer(owner.release());

    CHECK_FALSE(owner.is_open());
    CHECK(is_handle_open(importer.get()));
}

TEST_CASE("moving leaves exactly one owner") {
    ExportedHandle first(open_handle());
    const std::int64_t handle = first.get();

    const ExportedHandle second(std::move(first));

    CHECK(second.get() == handle);
    CHECK(is_handle_open(handle));
}

TEST_CASE("assigning closes the handle that was owned before") {
    ExportedHandle owner(open_handle());
    const std::int64_t replaced = owner.get();
    ExportedHandle replacement(open_handle());
    const std::int64_t kept = replacement.get();

    owner = std::move(replacement);

    CHECK_FALSE(is_handle_open(replaced));
    CHECK(owner.get() == kept);
    CHECK(is_handle_open(kept));
}

TEST_CASE("an owner without a handle closes nothing") {
    const ExportedHandle empty;

    CHECK_FALSE(empty.is_open());
}
