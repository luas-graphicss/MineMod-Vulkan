#include "cache/shader_cache.hpp"

#include <iterator>
#include <utility>

#include "minemod/abi/version.h"

namespace minemod::shaders {

namespace {

std::size_t bytes_of(std::string_view key, const std::vector<std::uint32_t>& spirv) noexcept {
    return key.size() + spirv.size() * sizeof(std::uint32_t);
}

}

std::string shader_cache_key(
    Stage stage, std::string_view compiler, const CoreShaderBindings& bindings, std::string_view source) {
    std::string key = "abi " + std::to_string(MINEMOD_ABI_VERSION_MAJOR) + "." + std::to_string(MINEMOD_ABI_VERSION_MINOR);
    key += "\ncompiler ";
    key += compiler;
    key += "\ncontract ";
    key += bindings.contract;
    key += " revision " + std::to_string(bindings.revision);
    key += "\nstage ";
    key += stage_name(stage);
    key += '\n';
    key += source;
    return key;
}

std::optional<std::vector<std::uint32_t>> ShaderCache::find(std::string_view key) {
    const auto indexed = index_.find(key);
    if (indexed == index_.end()) {
        return std::nullopt;
    }
    entries_.splice(entries_.begin(), entries_, indexed->second);
    return indexed->second->spirv;
}

void ShaderCache::store(std::string key, std::vector<std::uint32_t> spirv) {
    if (const auto replaced = index_.find(key); replaced != index_.end()) {
        forget(replaced->second);
    }
    const std::size_t bytes = bytes_of(key, spirv);
    if (bytes > max_bytes_) {
        return;
    }
    entries_.push_front(Entry{std::move(key), std::move(spirv)});
    index_.emplace(entries_.front().key, entries_.begin());
    stored_bytes_ += bytes;
    while (stored_bytes_ > max_bytes_) {
        forget(std::prev(entries_.end()));
    }
}

void ShaderCache::forget(std::list<Entry>::iterator entry) {
    stored_bytes_ -= bytes_of(entry->key, entry->spirv);
    index_.erase(entry->key);
    entries_.erase(entry);
}

}
