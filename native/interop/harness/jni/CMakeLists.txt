add_library(minemod_scene_host_jni SHARED
    host/host_natives.cpp
    library/jni_library.cpp
    "${PROJECT_SOURCE_DIR}/native/jni-bridge/src/library/native_registration.cpp")

target_include_directories(minemod_scene_host_jni
    PRIVATE "${CMAKE_CURRENT_SOURCE_DIR}" "${PROJECT_SOURCE_DIR}/native/jni-bridge/src")
target_link_libraries(minemod_scene_host_jni PRIVATE minemod::interop_harness minemod_jni_headers)
minemod_configure_vulkan_target(minemod_scene_host_jni)

set(minemod_scene_host_jni_output "$<1:${CMAKE_BINARY_DIR}/jni>")
set_target_properties(minemod_scene_host_jni PROPERTIES
    LIBRARY_OUTPUT_DIRECTORY "${minemod_scene_host_jni_output}"
    RUNTIME_OUTPUT_DIRECTORY "${minemod_scene_host_jni_output}")

if(UNIX)
    set(minemod_scene_host_jni_exports "${CMAKE_CURRENT_SOURCE_DIR}/exports.map")
    target_link_options(minemod_scene_host_jni PRIVATE "-Wl,--version-script=${minemod_scene_host_jni_exports}")
    set_property(TARGET minemod_scene_host_jni APPEND PROPERTY LINK_DEPENDS "${minemod_scene_host_jni_exports}")
    if(NOT MINEMOD_ENABLE_SANITIZERS)
        target_link_options(minemod_scene_host_jni PRIVATE -Wl,--no-undefined)
    endif()
endif()
