add_library(minemod_interop STATIC
    src/contexts/device_identity.cpp
    src/memory/exportable_memory.cpp
    src/memory/external_support.cpp
    src/memory/owned_descriptor.cpp
    src/memory/shared_buffer.cpp
    src/memory/shared_image.cpp)
add_library(minemod::interop ALIAS minemod_interop)

target_include_directories(minemod_interop PUBLIC src)
target_link_libraries(minemod_interop PUBLIC minemod::renderer)
minemod_configure_vulkan_target(minemod_interop)

if(MINEMOD_BUILD_TESTS)
    add_subdirectory(tests)
endif()
