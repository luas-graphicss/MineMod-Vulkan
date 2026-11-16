#pragma once

#include <cstddef>
#include <cstdint>
#include <list>
#include <optional>
#include <string>
#include <string_view>
#include <unordered_map>
#include <vector>

#include "adaptation/core_shader_bindings.hpp"
#include "source/stage.hpp"

namespace minemod::shaders {

std::string shader_cache_key(
    Stage stage, std::string_view compiler, const CoreShaderBindings& bindings, std::string_view source);

class ShaderCache {
public:
    explicit ShaderCache(std::size_t max_bytes) noexcept : max_bytes_(max_bytes) {}

    ShaderCache(const ShaderCache&) = delete;
    ShaderCache& operator=(const ShaderCache&) = delete;

    std::optional<std::vector<std::uint32_t>> find(std::string_view key);
    void store(std::string key, std::vector<std::uint32_t> spirv);

    std::size_t stored_bytes() const noexcept {
        return stored_bytes_;
    }

    std::size_t entry_count() const noexcept {
        return entries_.size();
    }

private:
    struct Entry {
        std::string key;
        std::vector<std::uint32_t> spirv;
    };

    void forget(std::list<Entry>::iterator entry);

    std::size_t max_bytes_;
    std::size_t stored_bytes_ = 0;
    std::list<Entry> entries_;
    std::unordered_map<std::string_view, std::list<Entry>::iterator> index_;
};

}
