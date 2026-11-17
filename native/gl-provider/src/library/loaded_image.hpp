#pragma once

#include <cstdint>
#include <filesystem>
#include <vector>

namespace minemod::provider {

struct ImageSegment {
    std::uintptr_t begin;
    std::uintptr_t end;

    bool contains(std::uintptr_t address) const noexcept {
        return address >= begin && address < end;
    }
};

std::vector<ImageSegment> segments_loaded_from(const std::filesystem::path& directory);

bool contains_address(const std::vector<ImageSegment>& segments, std::uintptr_t address) noexcept;

}
