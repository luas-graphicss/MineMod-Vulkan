#include <cstdint>
#include <stdexcept>
#include <utility>

#include <doctest.h>

#include "support/terrain_quads.hpp"
#include "terrain/pending_sections.hpp"
#include "terrain/section_mesh.hpp"
#include "work/job_epochs.hpp"

namespace {

using namespace minemod::renderer;
using namespace minemod::renderer::test_support;

constexpr SectionPosition section{1, 2, 3};
constexpr SectionPosition neighbor{2, 2, 3};
constexpr QuadSurface white{{255, 255, 255, 255}, {0.0f, 0.0f}, {brightest_packed_light, brightest_packed_light}};

SectionMesh quad_mesh() {
    SectionMesh mesh{};
    append_quad(mesh, TerrainLayer::solid, {0.0f, 0.0f, 1.0f, 1.0f, 0.0f}, white);
    return mesh;
}

PendingSection scheduled(JobEpochs& epochs, const SectionPosition& position) {
    return PendingSection{{position, epochs.schedule(position)}, quad_mesh()};
}

}

TEST_CASE("pending sections need room for at least one section") {
    CHECK_THROWS_AS(PendingSections(0), std::invalid_argument);
}

TEST_CASE("a full queue refuses another section and keeps the ones that wait") {
    JobEpochs epochs;
    PendingSections pending(1);
    REQUIRE(pending.offer(scheduled(epochs, section)));

    CHECK_FALSE(pending.offer(scheduled(epochs, neighbor)));

    CHECK(pending.size() == 1);
    CHECK(pending.find(section) != nullptr);
    CHECK(pending.find(neighbor) == nullptr);
}

TEST_CASE("the newest mesh of a section is the one that waits, even in a full queue") {
    JobEpochs epochs;
    PendingSections pending(1);
    PendingSection older = scheduled(epochs, section);
    PendingSection newer = scheduled(epochs, section);
    const std::uint64_t newest_revision = newer.job.stamp.revision;
    REQUIRE(pending.offer(std::move(older)));

    CHECK(pending.offer(std::move(newer)));
    CHECK(pending.offer(PendingSection{{section, JobStamp{0, 0, 0}}, quad_mesh()}));

    CHECK(pending.size() == 1);
    CHECK(pending.find(section)->job.stamp.revision == newest_revision);
}

TEST_CASE("jobs that are no longer current are discarded") {
    JobEpochs epochs;
    PendingSections pending(4);
    REQUIRE(pending.offer(scheduled(epochs, section)));
    REQUIRE(pending.offer(scheduled(epochs, neighbor)));

    epochs.schedule(neighbor);
    CHECK(pending.discard_stale(epochs) == 1);
    CHECK(pending.find(section) != nullptr);

    epochs.change_world();
    CHECK(pending.discard_stale(epochs) == 1);
    CHECK(pending.positions().empty());
}

TEST_CASE("a mesh that cannot be drawn is refused before it waits") {
    JobEpochs epochs;
    PendingSections pending(4);

    CHECK_THROWS_AS(pending.offer(PendingSection{{section, epochs.schedule(section)}, SectionMesh{}}), std::invalid_argument);
    CHECK(pending.size() == 0);
}

TEST_CASE("a removed section no longer waits") {
    JobEpochs epochs;
    PendingSections pending(4);
    REQUIRE(pending.offer(scheduled(epochs, section)));

    pending.remove(section);

    CHECK(pending.find(section) == nullptr);
    CHECK(pending.capacity() == 4);
}
