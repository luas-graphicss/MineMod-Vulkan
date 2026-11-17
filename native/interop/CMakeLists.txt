if(WIN32)
    set(minemod_interop_platform windows)
else()
    set(minemod_interop_platform linux)
endif()

add_library(minemod_interop STATIC
    src/contexts/device_identity.cpp
    src/handoff/ownership_transfer.cpp
    src/handoff/shared_resource_handoff.cpp
    src/memory/exportable_memory.cpp
    src/memory/exported_handle.cpp
    src/memory/external_support.cpp
    src/memory/shared_buffer.cpp
    src/memory/shared_image.cpp
    src/memory/${minemod_interop_platform}/handle_closing.cpp
    src/memory/${minemod_interop_platform}/memory_export.cpp
    src/sync/shared_semaphore.cpp
    src/sync/${minemod_interop_platform}/semaphore_export.cpp)
add_library(minemod::interop ALIAS minemod_interop)

target_include_directories(minemod_interop PUBLIC src)
target_link_libraries(minemod_interop PUBLIC minemod::renderer)
if(WIN32)
    target_compile_definitions(minemod_interop PRIVATE NOMINMAX WIN32_LEAN_AND_MEAN)
endif()
minemod_configure_vulkan_target(minemod_interop)

if(MINEMOD_BUILD_TESTS OR MINEMOD_BUILD_JNI_BRIDGE)
    add_subdirectory(harness)
endif()

if(MINEMOD_BUILD_TESTS)
    add_subdirectory(tests)
endif()
