add_library(minemod_jni SHARED
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
        src/scene/scene_calls.cpp
        src/scene/scene_host_calls.cpp
        src/scene/scene_session_calls.cpp)
    target_link_libraries(minemod_jni PRIVATE minemod::scene_session)
else()
    target_sources(minemod_jni PRIVATE src/scene/no_scene_calls.cpp)
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
