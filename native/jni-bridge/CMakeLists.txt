add_library(minemod_jni SHARED
    src/arrays/java_arrays.cpp
    src/bridge/bridge_calls.cpp
    src/buffers/direct_buffer.cpp
    src/fields/long_fields.cpp
    src/library/jni_library.cpp
    src/library/native_registration.cpp
    src/session/session_calls.cpp)

target_include_directories(minemod_jni PRIVATE src)
target_link_libraries(minemod_jni PRIVATE minemod::abi minemod_jni_headers)
if(TARGET minemod::scene_session)
    target_sources(minemod_jni PRIVATE
        src/metrics/scene_metrics_calls.cpp
        src/reload/scene_reload_calls.cpp
        src/scene/scene_calls.cpp
        src/scene/scene_host_calls.cpp
        src/scene/scene_session_calls.cpp
        src/sharing/scene_sharing_calls.cpp)
    if(CMAKE_SYSTEM_NAME STREQUAL "Linux")
        find_package(X11 REQUIRED)
        target_sources(minemod_jni PRIVATE
            src/sharing/exported_descriptors.cpp
            src/window/awt_surface_calls.cpp
            src/window/linux/awt_native_interface.cpp
            src/window/linux/drawing_surface.cpp)
        target_include_directories(minemod_jni SYSTEM PRIVATE "${X11_INCLUDE_DIR}")
        target_link_libraries(minemod_jni PRIVATE ${CMAKE_DL_LIBS})
    elseif(WIN32)
        target_sources(minemod_jni PRIVATE
            src/sharing/exported_nt_handles.cpp
            src/window/awt_surface_calls.cpp
            src/window/windows/awt_native_interface.cpp
            src/window/windows/drawing_surface.cpp)
        target_compile_definitions(minemod_jni PRIVATE NOMINMAX WIN32_LEAN_AND_MEAN)
    else()
        target_sources(minemod_jni PRIVATE
            src/sharing/no_exported_handles.cpp
            src/window/no_awt_surface_calls.cpp)
    endif()
    find_package(Vulkan REQUIRED)
    target_link_libraries(minemod_jni PRIVATE minemod::scene_session Vulkan::Vulkan)
else()
    target_sources(minemod_jni PRIVATE src/scene/no_scene_calls.cpp)
endif()
if(TARGET minemod::shader_service)
    target_sources(minemod_jni PRIVATE src/shaders/shader_service_calls.cpp)
    target_link_libraries(minemod_jni PRIVATE minemod::shader_service)
else()
    target_sources(minemod_jni PRIVATE src/shaders/no_shader_calls.cpp)
endif()
minemod_configure_target(minemod_jni)

set(minemod_jni_output "$<1:${CMAKE_BINARY_DIR}/jni>")
set_target_properties(minemod_jni PROPERTIES
    LIBRARY_OUTPUT_DIRECTORY "${minemod_jni_output}"
    RUNTIME_OUTPUT_DIRECTORY "${minemod_jni_output}")

if(UNIX)
    set(minemod_jni_exports "${CMAKE_CURRENT_SOURCE_DIR}/exports.map")
    target_link_options(minemod_jni PRIVATE "-Wl,--version-script=${minemod_jni_exports}")
    set_property(TARGET minemod_jni APPEND PROPERTY LINK_DEPENDS "${minemod_jni_exports}")
    if(NOT MINEMOD_ENABLE_SANITIZERS)
        target_link_options(minemod_jni PRIVATE -Wl,--no-undefined)
    endif()
endif()
