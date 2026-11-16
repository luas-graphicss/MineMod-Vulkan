#include <algorithm>
#include <vector>

#include <doctest.h>

#include "visibility/section_faces.hpp"
#include "visibility/section_occlusion.hpp"
#include "work/job_epochs.hpp"

namespace {

using namespace minemod::renderer;

constexpr SectionPosition camera_section{0, 0, 0};
constexpr SectionPosition east_of_camera{1, 0, 0};
constexpr SectionPosition two_east_of_camera{2, 0, 0};
constexpr SectionPosition above_east{1, 1, 0};

FaceConnections tunnel(SectionFace first, SectionFace second) {
    FaceConnections connections;
    connections.connect(first, second);
    return connections;
}

bool reaches(const std::vector<SectionPosition>& reachable, const SectionPosition& section) {
    return std::ranges::find(reachable, section) != reachable.end();
}

}

TEST_CASE("connecting two faces lets the view pass in both directions and nowhere else") {
    const FaceConnections connections = tunnel(SectionFace::west, SectionFace::east);

    CHECK(connections.connects(SectionFace::west, SectionFace::east));
    CHECK(connections.connects(SectionFace::east, SectionFace::west));
    CHECK_FALSE(connections.connects(SectionFace::west, SectionFace::up));
    CHECK(FaceConnections::open().connects(SectionFace::down, SectionFace::south));
    CHECK_FALSE(FaceConnections{}.connects(SectionFace::down, SectionFace::south));
}

TEST_CASE("open sections carry the view to every section behind them") {
    SectionOcclusion occlusion;
    occlusion.describe(camera_section, FaceConnections::open());
    occlusion.describe(east_of_camera, FaceConnections::open());
    occlusion.describe(two_east_of_camera, FaceConnections::open());

    const std::vector<SectionPosition> reachable = occlusion.reachable_from(camera_section);

    CHECK(reachable.size() == 3);
    CHECK(reachable.front() == camera_section);
    CHECK(reaches(reachable, two_east_of_camera));
}

TEST_CASE("a section that connects no faces is seen but hides what lies behind it") {
    SectionOcclusion occlusion;
    occlusion.describe(camera_section, FaceConnections::open());
    occlusion.describe(east_of_camera, FaceConnections{});
    occlusion.describe(two_east_of_camera, FaceConnections::open());

    const std::vector<SectionPosition> reachable = occlusion.reachable_from(camera_section);

    CHECK(reaches(reachable, east_of_camera));
    CHECK_FALSE(reaches(reachable, two_east_of_camera));
}

TEST_CASE("a tunnel carries the view only through the faces it connects") {
    SectionOcclusion occlusion;
    occlusion.describe(camera_section, FaceConnections::open());
    occlusion.describe(east_of_camera, tunnel(SectionFace::west, SectionFace::east));
    occlusion.describe(two_east_of_camera, FaceConnections::open());
    occlusion.describe(above_east, FaceConnections::open());

    const std::vector<SectionPosition> reachable = occlusion.reachable_from(camera_section);

    CHECK(reaches(reachable, two_east_of_camera));
    CHECK_FALSE(reaches(reachable, above_east));
}

TEST_CASE("the view never turns back toward the camera") {
    const SectionPosition two_above_east{1, 2, 0};
    const SectionPosition two_above_camera{0, 2, 0};
    SectionOcclusion occlusion;
    for (const SectionPosition& section :
        {camera_section, east_of_camera, above_east, two_above_east, two_above_camera}) {
        occlusion.describe(section, FaceConnections::open());
    }

    const std::vector<SectionPosition> reachable = occlusion.reachable_from(camera_section);

    CHECK(reaches(reachable, two_above_east));
    CHECK_FALSE(reaches(reachable, two_above_camera));
}

TEST_CASE("sections that were never described or were forgotten stop the view") {
    SectionOcclusion occlusion;
    occlusion.describe(camera_section, FaceConnections::open());
    occlusion.describe(east_of_camera, FaceConnections::open());
    occlusion.describe(two_east_of_camera, FaceConnections::open());

    occlusion.forget(east_of_camera);

    CHECK(occlusion.reachable_from(camera_section).size() == 1);
    CHECK(occlusion.size() == 2);
}

TEST_CASE("a camera outside every described section sees them all") {
    SectionOcclusion occlusion;
    occlusion.describe(east_of_camera, FaceConnections::open());
    occlusion.describe(two_east_of_camera, FaceConnections{});

    CHECK(occlusion.reachable_from(camera_section).size() == 2);

    occlusion.clear();
    CHECK(occlusion.reachable_from(camera_section).empty());
}
