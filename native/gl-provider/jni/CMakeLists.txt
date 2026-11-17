add_library(minemod_gl_provider_jni SHARED
    calls/call_natives.cpp
    calls/java_interceptor.cpp
    errors/java_exception.cpp
    library/jni_library.cpp
    lwjgl2/lookup_natives.cpp
    memory/memory_natives.cpp
    provider/provider_natives.cpp
    "${PROJECT_SOURCE_DIR}/native/jni-bridge/src/library/native_registration.cpp")

target_include_directories(minemod_gl_provider_jni
    PRIVATE "${CMAKE_CURRENT_SOURCE_DIR}" "${PROJECT_SOURCE_DIR}/native/jni-bridge/src")
target_link_libraries(minemod_gl_provider_jni PRIVATE minemod::gl_provider minemod_jni_headers)
minemod_configure_target(minemod_gl_provider_jni)

set(minemod_gl_provider_jni_output "$<1:${CMAKE_BINARY_DIR}/jni>")
set_target_properties(minemod_gl_provider_jni PROPERTIES
    LIBRARY_OUTPUT_DIRECTORY "${minemod_gl_provider_jni_output}"
    RUNTIME_OUTPUT_DIRECTORY "${minemod_gl_provider_jni_output}")

if(UNIX)
    set(minemod_gl_provider_jni_exports "${CMAKE_CURRENT_SOURCE_DIR}/exports.map")
    target_link_options(minemod_gl_provider_jni PRIVATE "-Wl,--version-script=${minemod_gl_provider_jni_exports}")
    set_property(TARGET minemod_gl_provider_jni APPEND PROPERTY LINK_DEPENDS "${minemod_gl_provider_jni_exports}")
    if(NOT MINEMOD_ENABLE_SANITIZERS)
        target_link_options(minemod_gl_provider_jni PRIVATE -Wl,--no-undefined)
    endif()
endif()
