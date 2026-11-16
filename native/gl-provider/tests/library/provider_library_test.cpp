#include <unistd.h>

#include <array>
#include <filesystem>
#include <span>
#include <string>

#include <doctest.h>

#include "library/provider_library.hpp"
#include "support/fake_provider_library.hpp"

namespace {

using namespace minemod::provider;
using minemod::provider::testing::fake_provider_directory;
using minemod::provider::testing::open_fake_provider;

const std::string entry = "libfake_provider.so";
const std::array<std::string, 1> dependencies{"libfake_dependency.so"};

class LinkedDirectory {
public:
    LinkedDirectory()
        : path_(std::filesystem::temp_directory_path() / ("minemod-provider-links-" + std::to_string(::getpid()))) {
        std::filesystem::create_directory(path_);
        std::filesystem::create_symlink(fake_provider_directory() / entry, path_ / entry);
    }
    LinkedDirectory(const LinkedDirectory&) = delete;
    LinkedDirectory& operator=(const LinkedDirectory&) = delete;
    ~LinkedDirectory() {
        std::filesystem::remove_all(path_);
    }

    const std::filesystem::path& path() const noexcept {
        return path_;
    }

private:
    std::filesystem::path path_;
};

}

TEST_CASE("the entry library resolves its symbols once its dependencies were loaded from the same directory") {
    const ProviderLibrary library = open_fake_provider();

    CHECK(library.symbol("glGetError") != nullptr);
    CHECK(library.symbol("glNoSuchFunction") == nullptr);
    CHECK(library.directory() == std::filesystem::canonical(fake_provider_directory()));
}

TEST_CASE("the entry library is refused when a dependency was left out of the bundle") {
    CHECK_THROWS_AS(
        ProviderLibrary(fake_provider_directory(), std::span<const std::string>{}, entry), LibraryLoadError);
}

TEST_CASE("a library is named by file name only") {
    const std::filesystem::path directory = fake_provider_directory();

    CHECK_THROWS_AS(ProviderLibrary(directory, dependencies, "../provider/" + entry), LibraryLoadError);
    CHECK_THROWS_AS(ProviderLibrary(directory, dependencies, (directory / entry).string()), LibraryLoadError);
    CHECK_THROWS_AS(ProviderLibrary(directory, dependencies, ""), LibraryLoadError);
    CHECK_THROWS_AS(ProviderLibrary(directory, dependencies, ".."), LibraryLoadError);
}

TEST_CASE("a missing library and a missing directory are reported") {
    CHECK_THROWS_AS(
        ProviderLibrary(fake_provider_directory(), dependencies, "libabsent.so"), LibraryLoadError);
    CHECK_THROWS_AS(
        ProviderLibrary(fake_provider_directory() / "absent", dependencies, entry), LibraryLoadError);
}

TEST_CASE("a symbolic link in the provider directory is not followed") {
    const LinkedDirectory links;

    CHECK_THROWS_AS(ProviderLibrary(links.path(), std::span<const std::string>{}, entry), LibraryLoadError);
}

TEST_CASE("a moved library keeps its symbols and leaves the source empty") {
    ProviderLibrary library = open_fake_provider();

    const ProviderLibrary moved = std::move(library);

    CHECK(moved.symbol("glGetError") != nullptr);
    CHECK(library.symbol("glGetError") == nullptr);
}
