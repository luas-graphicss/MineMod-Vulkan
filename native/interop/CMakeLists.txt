add_library(minemod_interop STATIC
    src/contexts/device_identity.cpp
    src/handoff/ownership_transfer.cpp
    src/handoff/shared_resource_handoff.cpp
    src/memory/exportable_memory.cpp
    src/memory/external_support.cpp
    src/memory/owned_descriptor.cpp
    src/memory/shared_buffer.cpp
    src/memory/shared_image.cpp
    src/sync/shared_semaphore.cpp)
add_library(minemod::interop ALIAS minemod_interop)

target_include_directories(minemod_interop PUBLIC src)
target_link_libraries(minemod_interop PUBLIC minemod::renderer)
minemod_configure_vulkan_target(minemod_interop)

if(MINEMOD_BUILD_TESTS OR MINEMOD_BUILD_JNI_BRIDGE)
    add_subdirectory(harness)
endif()

if(MINEMOD_BUILD_TESTS)
    add_subdirectory(tests)
endif()
