#include "library/loaded_image.hpp"

#include <link.h>

#include <exception>

namespace minemod::provider {

namespace {

struct Collection {
    const std::filesystem::path& directory;
    std::vector<ImageSegment> segments;
    std::exception_ptr failure;
};

bool is_loaded_from(const std::filesystem::path& directory, const char* object_name) {
    if (object_name == nullptr || *object_name == '\0') {
        return false;
    }
    std::error_code failure;
    const std::filesystem::path object = std::filesystem::canonical(object_name, failure);
    return !failure && object.parent_path() == directory;
}

void collect_segments(const dl_phdr_info& object, std::vector<ImageSegment>& segments) {
    for (ElfW(Half) index = 0; index < object.dlpi_phnum; ++index) {
        const ElfW(Phdr)& header = object.dlpi_phdr[index];
        if (header.p_type != PT_LOAD || header.p_memsz == 0) {
            continue;
        }
        const auto begin = static_cast<std::uintptr_t>(object.dlpi_addr + header.p_vaddr);
        segments.push_back(ImageSegment{begin, begin + static_cast<std::uintptr_t>(header.p_memsz)});
    }
}

int visit_object(dl_phdr_info* object, std::size_t, void* context) noexcept {
    auto& collection = *static_cast<Collection*>(context);
    try {
        if (is_loaded_from(collection.directory, object->dlpi_name)) {
            collect_segments(*object, collection.segments);
        }
        return 0;
    } catch (...) {
        collection.failure = std::current_exception();
        return 1;
    }
}

}

std::vector<ImageSegment> segments_loaded_from(const std::filesystem::path& directory) {
    Collection collection{directory, {}, nullptr};
    ::dl_iterate_phdr(&visit_object, &collection);
    if (collection.failure) {
        std::rethrow_exception(collection.failure);
    }
    return std::move(collection.segments);
}

}
