#include <doctest.h>

#include "work/job_epochs.hpp"

namespace {

using namespace minemod::renderer;

constexpr SectionPosition spawn_section{0, 4, 0};
constexpr SectionPosition far_section{-12, 4, 31};

}

TEST_CASE("a job stays current while its section, world and resources are unchanged") {
    JobEpochs epochs;
    const JobStamp spawn_job = epochs.schedule(spawn_section);
    const JobStamp far_job = epochs.schedule(far_section);

    CHECK(epochs.is_current(spawn_section, spawn_job));
    CHECK(epochs.is_current(far_section, far_job));
    CHECK_FALSE(epochs.is_current(far_section, spawn_job));
}

TEST_CASE("scheduling a section again makes its earlier job stale") {
    JobEpochs epochs;
    const JobStamp first_job = epochs.schedule(spawn_section);
    const JobStamp second_job = epochs.schedule(spawn_section);

    CHECK_FALSE(epochs.is_current(spawn_section, first_job));
    CHECK(epochs.is_current(spawn_section, second_job));
}

TEST_CASE("a job for a section that was unloaded stays stale after the position loads again") {
    JobEpochs epochs;
    const JobStamp before_unload = epochs.schedule(spawn_section);

    epochs.unload(spawn_section);
    CHECK_FALSE(epochs.is_current(spawn_section, before_unload));

    const JobStamp after_reload = epochs.schedule(spawn_section);
    CHECK_FALSE(epochs.is_current(spawn_section, before_unload));
    CHECK(epochs.is_current(spawn_section, after_reload));
}

TEST_CASE("changing the world makes every earlier job stale") {
    JobEpochs epochs;
    const JobStamp overworld_job = epochs.schedule(spawn_section);

    epochs.change_world();

    CHECK_FALSE(epochs.is_current(spawn_section, overworld_job));
    CHECK(epochs.is_current(spawn_section, epochs.schedule(spawn_section)));
}

TEST_CASE("reloading resources makes every earlier job stale") {
    JobEpochs epochs;
    const JobStamp old_atlas_job = epochs.schedule(spawn_section);

    epochs.reload_resources();

    CHECK_FALSE(epochs.is_current(spawn_section, old_atlas_job));
    CHECK(epochs.is_current(spawn_section, epochs.schedule(spawn_section)));
}
