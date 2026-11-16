#include <array>
#include <cstdint>
#include <vector>

#include <doctest.h>

#include "math/vector.hpp"
#include "support/terrain_quads.hpp"
#include "support/terrain_stage.hpp"
#include "terrain/section_mesh.hpp"
#include "terrain/section_space.hpp"
#include "visibility/section_faces.hpp"
#include "visibility/section_occlusion.hpp"
#include "work/job_epochs.hpp"

namespace {

using namespace minemod::renderer;
using namespace minemod::renderer::test_support;

constexpr WorldPosition camera{8.0, 8.0, 0.0};
constexpr std::array<SectionPosition, 3> row_ahead{{{0, 0, -1}, {0, 0, -2}, {0, 0, -3}}};
constexpr QuadSurface white{{255, 255, 255, 255}, {0.5f, 0.5f}, {brightest_packed_light, brightest_packed_light}};

SectionMesh wall_mesh() {
    SectionMesh mesh{};
    append_quad(mesh, TerrainLayer::solid, {0.0f, 0.0f, 16.0f, 16.0f, 8.0f}, white);
    return mesh;
}

FaceConnections passage_ahead() {
    FaceConnections connections;
    connections.connect(SectionFace::south, SectionFace::north);
    return connections;
}

class RowOfSections {
public:
    RowOfSections() {
        occlusion_.describe(section_containing(camera), FaceConnections::open());
        for (const SectionPosition& section : row_ahead) {
            stage_.publish(section, wall_mesh());
            occlusion_.describe(section, FaceConnections::open());
        }
    }

    void describe_nearest(const FaceConnections& connections) {
        occlusion_.describe(row_ahead.front(), connections);
    }

    std::uint32_t draws_of_reachable_sections() {
        const std::vector<SectionPosition> reachable = occlusion_.reachable_from(section_containing(camera));
        stage_.render({camera, reachable});
        return stage_.draw_count();
    }

    std::uint32_t draws_of_every_section() {
        stage_.render({camera});
        return stage_.draw_count();
    }

private:
    TerrainStage stage_;
    SectionOcclusion occlusion_;
};

}

TEST_CASE("terrain behind a section that connects no faces is not drawn") {
    RowOfSections row;
    row.describe_nearest(FaceConnections{});

    CHECK(row.draws_of_reachable_sections() == 1);
    CHECK(row.draws_of_every_section() == 3);
}

TEST_CASE("terrain behind a section is drawn once a passage crosses it") {
    RowOfSections row;
    row.describe_nearest(passage_ahead());

    CHECK(row.draws_of_reachable_sections() == 3);
}
