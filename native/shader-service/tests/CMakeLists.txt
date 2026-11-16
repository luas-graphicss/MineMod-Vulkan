find_program(MINEMOD_GLSLANG_VALIDATOR glslangValidator REQUIRED)

add_executable(minemod_shader_service_tests
    abi/shader_service_abi_test.cpp
    adaptation/core_shader_adapter_test.cpp
    cache/shader_cache_test.cpp
    compiler/compiler_log_test.cpp
    compiler/glslang_process_compiler_test.cpp
    process/child_process_test.cpp
    service/core_shader_compilation_test.cpp
    source/include_expansion_test.cpp
    support/core_shader_samples.cpp
    support/scratch_directory.cpp
    syntax/glsl_tokens_test.cpp
    syntax/interface_declarations_test.cpp
    syntax/line_map_test.cpp)
target_include_directories(minemod_shader_service_tests PRIVATE "${CMAKE_CURRENT_SOURCE_DIR}")
target_compile_definitions(minemod_shader_service_tests PRIVATE
    MINEMOD_GLSLANG_VALIDATOR="${MINEMOD_GLSLANG_VALIDATOR}"
    MINEMOD_SCENE_VERTEX_SHADER="${PROJECT_SOURCE_DIR}/shaders/native/scene.vert")
target_link_libraries(minemod_shader_service_tests
    PRIVATE minemod::shader_service minemod::renderer minemod_doctest_main)
minemod_configure_target(minemod_shader_service_tests)

add_test(NAME shader_service COMMAND minemod_shader_service_tests)
