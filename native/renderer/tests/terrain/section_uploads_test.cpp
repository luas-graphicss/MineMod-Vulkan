#include <cstddef>
#include <cstdint>
#include <stdexcept>
#include <string>

#include <doctest.h>

#include "lifetime/retirement_queue.hpp"
#include "math/vector.hpp"
#include "support/terrain_quads.hpp"
#include "support/validated_device.hpp"
#include "terrain/pending_sections.hpp"
#include "terrain/section_mesh.hpp"
#include "terrain/section_uploads.hpp"
#include "terrain/terrain_sections.hpp"
#include "upload/upload_arena.hpp"
#include "work/job_epochs.hpp"

namespace {

using namespace minemod::renderer;
using namespace minemod::renderer::test_support;

constexpr WorldPosition camera{8.0, 8.0, 0.0};
constexpr SectionPosition near_section{0, 0, -1};
constexpr SectionPosition middle_section{0, 0, -3};
constexpr SectionPosition far_section{0, 0, -6};
constexpr VkDeviceSize room_for_two_sections = 300;
constexpr VkDeviceSize room_for_no_section = 64;
constexpr QuadSurface white{{255, 255, 255, 255}, {0.0f, 0.0f}, {brightest_packed_light, brightest_packed_light}};

SectionMesh quad_mesh() {
    SectionMesh mesh{};
    append_quad(mesh, TerrainLayer::solid, {0.0f, 0.0f, 1.0f, 1.0f, 0.0f}, white);
    return mesh;
}

class UploadingTerrain {
public:
    explicit UploadingTerrain(VkDeviceSize arena_capacity)
        : arena_(gpu_.device(), arena_capacity), sections_(retirements_), uploads_(gpu_.device(), pending_, sections_) {}

    void offer(const SectionPosition& section) {
        REQUIRE(pending_.offer(PendingSection{{section, epochs_.schedule(section)}, quad_mesh()}));
    }

    UploadedSections record_frame(std::uint64_t frame) {
        UploadedSections uploaded{};
        arena_.reset();
        gpu_.device().submit([&](VkCommandBuffer commands) {
            uploaded = uploads_.record({commands, frame, camera}, arena_, epochs_);
        });
        return uploaded;
    }

    UploadedSections record_without_commands() {
        return uploads_.record({VK_NULL_HANDLE, 0, camera}, arena_, epochs_);
    }

    void leave_world() {
        epochs_.change_world();
    }

    bool is_published(const SectionPosition& section) const {
        return sections_.find(section) != nullptr;
    }

    std::size_t waiting() const noexcept {
        return pending_.size();
    }

    std::string validation_text() const {
        return gpu_.validation_text();
    }

private:
    ValidatedDevice gpu_;
    UploadArena arena_;
    RetirementQueue retirements_;
    TerrainSections sections_;
    JobEpochs epochs_;
    PendingSections pending_{8};
    SectionUploads uploads_;
};

}

TEST_CASE("sections upload nearest first and the ones the arena has no room for wait for the next frame") {
    UploadingTerrain terrain(room_for_two_sections);
    terrain.offer(far_section);
    terrain.offer(near_section);
    terrain.offer(middle_section);

    CHECK(terrain.record_frame(0) == UploadedSections{2, 0, 1});
    CHECK(terrain.is_published(near_section));
    CHECK(terrain.is_published(middle_section));
    CHECK_FALSE(terrain.is_published(far_section));

    CHECK(terrain.record_frame(1) == UploadedSections{1, 0, 0});
    CHECK(terrain.is_published(far_section));
    CHECK(terrain.validation_text() == "");
}

TEST_CASE("sections of a world that was left are discarded instead of uploaded") {
    UploadingTerrain terrain(room_for_two_sections);
    terrain.offer(near_section);
    terrain.offer(far_section);

    terrain.leave_world();

    CHECK(terrain.record_frame(0) == UploadedSections{0, 2, 0});
    CHECK_FALSE(terrain.is_published(near_section));
}

TEST_CASE("a mesh larger than an empty arena is rejected and stops waiting") {
    UploadingTerrain terrain(room_for_no_section);
    terrain.offer(near_section);

    CHECK_THROWS_AS(terrain.record_without_commands(), std::length_error);
    CHECK(terrain.waiting() == 0);
}
