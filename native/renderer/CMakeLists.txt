find_package(Vulkan REQUIRED)
if(NOT Vulkan_GLSLANG_VALIDATOR_EXECUTABLE)
    message(FATAL_ERROR "glslangValidator is required to compile the renderer shaders")
endif()

function(minemod_configure_vulkan_target target)
    minemod_configure_target(${target})
    if(NOT MSVC)
        target_compile_options(${target} PRIVATE -Wno-missing-field-initializers)
    endif()
endfunction()

function(minemod_add_shader_library target)
    set(shader_headers "${CMAKE_CURRENT_BINARY_DIR}/${target}")
    set(generated_headers "")
    foreach(shader IN LISTS ARGN)
        string(REPLACE "." "_" shader_symbol "${shader}")
        set(shader_source "${PROJECT_SOURCE_DIR}/shaders/native/${shader}")
        set(shader_header "${shader_headers}/${shader_symbol}.h")
        add_custom_command(
            OUTPUT "${shader_header}"
            COMMAND "${CMAKE_COMMAND}" -E make_directory "${shader_headers}"
            COMMAND "${Vulkan_GLSLANG_VALIDATOR_EXECUTABLE}" -V --quiet --target-env vulkan1.3
                --vn "${shader_symbol}" -o "${shader_header}" "${shader_source}"
            DEPENDS "${shader_source}"
            VERBATIM)
        list(APPEND generated_headers "${shader_header}")
    endforeach()

    add_custom_target(${target}_headers DEPENDS ${generated_headers})
    add_library(${target} INTERFACE)
    add_dependencies(${target} ${target}_headers)
    target_include_directories(${target} INTERFACE "${shader_headers}")
endfunction()

add_library(minemod_renderer STATIC
    src/descriptors/descriptor_layout.cpp
    src/descriptors/descriptor_pools.cpp
    src/device/device.cpp
    src/device/instance.cpp
    src/device/physical_device.cpp
    src/device/vulkan_error.cpp
    src/frame/frame_readback.cpp
    src/frame/frame_targets.cpp
    src/frame/frames_in_flight.cpp
    src/graph/graph_images.cpp
    src/graph/graph_plan.cpp
    src/graph/image_state.cpp
    src/graph/pass_order.cpp
    src/graph/planned_frame.cpp
    src/graph/render_graph.cpp
    src/graph/transient_images.cpp
    src/graph/transient_slots.cpp
    src/lifetime/retirement_queue.cpp
    src/math/matrix.cpp
    src/metrics/frame_metrics.cpp
    src/pipelines/graphics_pipeline_state.cpp
    src/pipelines/pipeline_cache.cpp
    src/pipelines/shader_module.cpp
    src/presentation/frame_lifecycle.cpp
    src/presentation/headless_surface.cpp
    src/presentation/swapchain.cpp
    src/resources/buffer.cpp
    src/resources/image.cpp
    src/resources/image_layout.cpp
    src/resources/memory_type.cpp
    src/shaders/core_shader_contract.cpp
    src/shaders/shader_interface.cpp
    src/shaders/spirv_module.cpp
    src/terrain/section_mesh.cpp
    src/terrain/section_selection.cpp
    src/terrain/section_space.cpp
    src/terrain/terrain_vertex.cpp
    src/textures/atlas_layout.cpp
    src/textures/mip_chain.cpp
    src/textures/sampler_cache.cpp
    src/textures/sprite_animation.cpp
    src/textures/texture.cpp
    src/upload/arena_layout.cpp
    src/upload/upload_arena.cpp
    src/visibility/frustum.cpp
    src/work/job_epochs.cpp)
add_library(minemod::renderer ALIAS minemod_renderer)

target_include_directories(minemod_renderer PUBLIC src)
target_link_libraries(minemod_renderer PUBLIC Vulkan::Vulkan)
minemod_configure_vulkan_target(minemod_renderer)

add_subdirectory(harness)

if(MINEMOD_BUILD_TESTS)
    add_subdirectory(tests)
endif()
