set(minemod_shader_sources "${PROJECT_SOURCE_DIR}/shaders/native")
set(minemod_shader_headers "${CMAKE_CURRENT_BINARY_DIR}/shaders")

foreach(stage vert frag)
    set(shader_source "${minemod_shader_sources}/proof_scene.${stage}")
    set(shader_header "${minemod_shader_headers}/proof_scene_${stage}.h")
    add_custom_command(
        OUTPUT "${shader_header}"
        COMMAND "${CMAKE_COMMAND}" -E make_directory "${minemod_shader_headers}"
        COMMAND "${Vulkan_GLSLANG_VALIDATOR_EXECUTABLE}" -V --quiet --target-env vulkan1.3
            --vn "proof_scene_${stage}" -o "${shader_header}" "${shader_source}"
        DEPENDS "${shader_source}"
        VERBATIM)
    list(APPEND minemod_proof_scene_shaders "${shader_header}")
endforeach()

add_library(minemod_harness_run STATIC
    src/cli/harness_options.cpp
    src/cli/harness_report_text.cpp
    src/run/harness_run.cpp
    src/scene/checker_texture.cpp
    src/scene/proof_pipeline.cpp
    src/scene/proof_scene.cpp
    ${minemod_proof_scene_shaders})
target_include_directories(minemod_harness_run PUBLIC src PRIVATE "${minemod_shader_headers}")
target_link_libraries(minemod_harness_run PUBLIC minemod::renderer)
minemod_configure_vulkan_target(minemod_harness_run)

add_executable(minemod_harness src/main.cpp)
target_link_libraries(minemod_harness PRIVATE minemod_harness_run)
minemod_configure_vulkan_target(minemod_harness)
