add_library(minemod_shader_service STATIC
    src/abi/shader_service_abi.cpp
    src/adaptation/core_shader_adapter.cpp
    src/adaptation/core_shader_bindings.cpp
    src/cache/shader_cache.cpp
    src/compiler/compiler_log.cpp
    src/compiler/glslang_process_compiler.cpp
    src/process/child_process.cpp
    src/service/shader_service.cpp
    src/source/include_expansion.cpp
    src/syntax/glsl_tokens.cpp
    src/syntax/interface_declarations.cpp
    src/syntax/line_map.cpp)
add_library(minemod::shader_service ALIAS minemod_shader_service)
target_include_directories(minemod_shader_service PUBLIC include src)
target_link_libraries(minemod_shader_service PUBLIC minemod::abi)
minemod_configure_target(minemod_shader_service)

if(MINEMOD_BUILD_TESTS)
    add_subdirectory(tests)
endif()
