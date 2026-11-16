#pragma once

#include <filesystem>

namespace minemod::shaders::test_support {

class ScratchDirectory {
public:
    ScratchDirectory();
    ~ScratchDirectory();

    ScratchDirectory(const ScratchDirectory&) = delete;
    ScratchDirectory& operator=(const ScratchDirectory&) = delete;

    const std::filesystem::path& path() const noexcept {
        return path_;
    }

private:
    std::filesystem::path path_;
};

}
