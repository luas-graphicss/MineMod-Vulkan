#include <stdexcept>

#include <doctest.h>

#include "lifetime/retirement_queue.hpp"
#include "support/terrain_quads.hpp"
#include "support/validated_device.hpp"
#include "terrain/section_buffers.hpp"
#include "terrain/terrain_sections.hpp"
#include "upload/upload_arena.hpp"
#include "work/job_epochs.hpp"

namespace {

using namespace minemod::renderer;
using namespace minemod::renderer::test_support;

constexpr SectionPosition section{1, 2, 3};
constexpr SectionPosition neighbor{2, 2, 3};
constexpr QuadPlacement unit_quad{0.0f, 0.0f, 1.0f, 1.0f, 0.0f};
constexpr QuadSurface white{{255, 255, 255, 255}, {0.0f, 0.0f}, {brightest_packed_light, brightest_packed_light}};

SectionMesh quad_mesh() {
    SectionMesh mesh{};
    append_quad(mesh, TerrainLayer::solid, unit_quad, white);
    return mesh;
}

}

TEST_CASE("a section job that was superseded does not publish its buffers") {
    const ValidatedDevice gpu;
    RetirementQueue retirements;
    TerrainSections sections(retirements);
    JobEpochs epochs;
    const JobStamp superseded = epochs.schedule(section);
    const JobStamp current = epochs.schedule(section);

    CHECK_FALSE(sections.publish(epochs, {section, superseded}, SectionBuffers(gpu.device(), quad_mesh()), 0));
    CHECK(sections.find(section) == nullptr);

    CHECK(sections.publish(epochs, {section, current}, SectionBuffers(gpu.device(), quad_mesh()), 0));
    CHECK(sections.find(section) != nullptr);
    CHECK(retirements.pending_count() == 0);
}

TEST_CASE("jobs from a previous world or resource generation are dropped") {
    const ValidatedDevice gpu;
    RetirementQueue retirements;
    TerrainSections sections(retirements);
    JobEpochs epochs;

    const JobStamp before_world_change = epochs.schedule(section);
    epochs.change_world();
    CHECK_FALSE(sections.publish(epochs, {section, before_world_change}, SectionBuffers(gpu.device(), quad_mesh()), 0));

    const JobStamp before_reload = epochs.schedule(section);
    epochs.reload_resources();
    CHECK_FALSE(sections.publish(epochs, {section, before_reload}, SectionBuffers(gpu.device(), quad_mesh()), 0));

    CHECK(sections.size() == 0);
}

TEST_CASE("replaced buffers stay alive until the last frame that drew them completes") {
    const ValidatedDevice gpu;
    RetirementQueue retirements;
    TerrainSections sections(retirements);
    JobEpochs epochs;
    REQUIRE(sections.publish(epochs, {section, epochs.schedule(section)}, SectionBuffers(gpu.device(), quad_mesh()), 0));

    REQUIRE(sections.publish(epochs, {section, epochs.schedule(section)}, SectionBuffers(gpu.device(), quad_mesh()), 5));

    CHECK(sections.size() == 1);
    CHECK(retirements.pending_count() == 1);
    CHECK(retirements.release_through(4) == 0);
    CHECK(retirements.release_through(5) == 1);
}

TEST_CASE("unloading retires the buffers and forgets the section") {
    const ValidatedDevice gpu;
    RetirementQueue retirements;
    TerrainSections sections(retirements);
    JobEpochs epochs;
    REQUIRE(sections.publish(epochs, {section, epochs.schedule(section)}, SectionBuffers(gpu.device(), quad_mesh()), 0));

    sections.unload(section, 7);
    sections.unload(section, 8);

    CHECK(sections.find(section) == nullptr);
    CHECK(retirements.pending_count() == 1);
}

TEST_CASE("leaving a world retires every section") {
    const ValidatedDevice gpu;
    RetirementQueue retirements;
    TerrainSections sections(retirements);
    JobEpochs epochs;
    REQUIRE(sections.publish(epochs, {section, epochs.schedule(section)}, SectionBuffers(gpu.device(), quad_mesh()), 0));
    REQUIRE(sections.publish(epochs, {neighbor, epochs.schedule(neighbor)}, SectionBuffers(gpu.device(), quad_mesh()), 0));

    sections.unload_all(9);

    CHECK(sections.size() == 0);
    CHECK(sections.positions().empty());
    CHECK(retirements.pending_count() == 2);
}

TEST_CASE("an upload that does not match the buffers is rejected before anything is recorded") {
    const ValidatedDevice gpu;
    const SectionBuffers buffers(gpu.device(), quad_mesh());
    const StagedSection mismatched{{0, 1}, {0, 1}};

    CHECK_THROWS_AS(buffers.record_upload(VK_NULL_HANDLE, VK_NULL_HANDLE, mismatched), std::invalid_argument);
}

TEST_CASE("a section that does not fit the upload arena is not staged") {
    const ValidatedDevice gpu;
    UploadArena arena(gpu.device(), 64);

    CHECK_FALSE(stage_section(arena, quad_mesh()).has_value());
}
