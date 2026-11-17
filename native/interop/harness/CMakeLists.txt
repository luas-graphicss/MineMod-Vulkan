add_library(minemod_interop_harness STATIC
    src/harness/scene_image.cpp
    src/harness/shared_scene_host.cpp)
add_library(minemod::interop_harness ALIAS minemod_interop_harness)

target_include_directories(minemod_interop_harness PUBLIC src)
target_link_libraries(minemod_interop_harness PUBLIC minemod::interop)
minemod_configure_vulkan_target(minemod_interop_harness)

if(MINEMOD_BUILD_JNI_BRIDGE AND CMAKE_SYSTEM_NAME STREQUAL "Linux")
    add_subdirectory(jni)
endif()
