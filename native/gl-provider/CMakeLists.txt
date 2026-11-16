find_package(Python3 REQUIRED COMPONENTS Interpreter)

set(minemod_gl_registry "${CMAKE_CURRENT_BINARY_DIR}/registry/gl.xml")
file(DOWNLOAD
    https://raw.githubusercontent.com/KhronosGroup/OpenGL-Registry/1cdd228e34966dd6b95bd203e9f84faba0f371a1/xml/gl.xml
    "${minemod_gl_registry}"
    EXPECTED_HASH SHA256=b9ca2cfa5c676e901c20d34af3407f1687cde0f1336a5ff7a8974d04c7494ad3
    TLS_VERIFY ON)

set(minemod_gl_catalog
    "${PROJECT_SOURCE_DIR}/compatibility/dispatch/src/main/resources/io/github/luasgraphicss/minemod/compatibility/dispatch/catalog")
set(minemod_gl_generator "${CMAKE_CURRENT_SOURCE_DIR}/generator/generate_call_table.py")
set(minemod_gl_provider_only "${CMAKE_CURRENT_SOURCE_DIR}/generator/provider-only-calls.txt")
set(minemod_gl_call_table "${CMAKE_CURRENT_BINARY_DIR}/generated/call_table_records.cpp")

add_custom_command(
    OUTPUT "${minemod_gl_call_table}"
    COMMAND "${CMAKE_COMMAND}" -E make_directory "${CMAKE_CURRENT_BINARY_DIR}/generated"
    COMMAND Python3::Interpreter "${minemod_gl_generator}"
        --registry "${minemod_gl_registry}"
        --calls "${minemod_gl_catalog}/gl-calls.txt"
        --extensions "${minemod_gl_catalog}/gl-extensions.txt"
        --provider-only "${minemod_gl_provider_only}"
        --output "${minemod_gl_call_table}"
    DEPENDS
        "${minemod_gl_generator}"
        "${minemod_gl_registry}"
        "${minemod_gl_catalog}/gl-calls.txt"
        "${minemod_gl_catalog}/gl-extensions.txt"
        "${minemod_gl_provider_only}"
    COMMENT "Generating the OpenGL call table from the Khronos registry"
    VERBATIM)

add_library(minemod_gl_provider STATIC
    src/calls/call_gate.cpp
    src/calls/call_table.cpp
    src/context/egl_entry_points.cpp
    src/context/provider_display.cpp
    src/library/driver_settings.cpp
    src/library/loaded_image.cpp
    src/library/provider_library.cpp
    src/session/provider_session.cpp
    "${minemod_gl_call_table}")
add_library(minemod::gl_provider ALIAS minemod_gl_provider)

target_include_directories(minemod_gl_provider PUBLIC src)
target_link_libraries(minemod_gl_provider PUBLIC ${CMAKE_DL_LIBS})
minemod_configure_target(minemod_gl_provider)

if(MINEMOD_BUILD_JNI_BRIDGE)
    add_subdirectory(jni)
endif()

if(MINEMOD_BUILD_TESTS)
    add_subdirectory(tests)
endif()
