add_library(minemod_renderer_test_support STATIC
    support/graph_passes.cpp
    support/pixel_colors.cpp
    support/presenting_device.cpp
    support/scene_vertices.cpp
    support/terrain_quads.cpp
    support/validated_device.cpp)
target_include_directories(minemod_renderer_test_support PUBLIC "${CMAKE_CURRENT_SOURCE_DIR}")
target_link_libraries(minemod_renderer_test_support PUBLIC minemod::renderer)
minemod_configure_vulkan_target(minemod_renderer_test_support)

add_executable(minemod_renderer_tests
    device_requirements_test.cpp
    harness_options_test.cpp
    harness_run_test.cpp
    synchronization_validation_test.cpp)
target_link_libraries(minemod_renderer_tests
    PRIVATE minemod_harness_run minemod_renderer_test_support minemod_doctest_main)
minemod_configure_vulkan_target(minemod_renderer_tests)

add_executable(minemod_render_graph_tests
    graph/graph_declaration_test.cpp
    graph/image_transitions_test.cpp
    graph/pass_order_test.cpp
    graph/transient_slots_test.cpp)
target_include_directories(minemod_render_graph_tests PRIVATE "${CMAKE_CURRENT_SOURCE_DIR}")
target_link_libraries(minemod_render_graph_tests PRIVATE minemod::renderer minemod_doctest_main)
minemod_configure_vulkan_target(minemod_render_graph_tests)

add_executable(minemod_render_graph_device_tests graph/graph_frame_test.cpp)
target_link_libraries(minemod_render_graph_device_tests PRIVATE minemod_renderer_test_support minemod_doctest_main)
minemod_configure_vulkan_target(minemod_render_graph_device_tests)

minemod_add_shader_library(minemod_sprite_shaders sprite.vert sprite.frag)

add_executable(minemod_renderer_resource_tests
    lifetime/retirement_queue_test.cpp
    metrics/frame_metrics_test.cpp
    presentation/frame_lifecycle_test.cpp
    reload/resource_generations_test.cpp
    shaders/core_shader_contract_test.cpp
    shaders/shader_interface_test.cpp
    shaders/spirv_module_test.cpp
    textures/atlas_layout_test.cpp
    textures/mip_chain_test.cpp
    textures/sprite_animation_test.cpp
    upload/arena_layout_test.cpp
    work/bounded_queue_test.cpp
    work/job_epochs_test.cpp)
target_include_directories(minemod_renderer_resource_tests PRIVATE "${CMAKE_CURRENT_SOURCE_DIR}")
target_link_libraries(minemod_renderer_resource_tests
    PRIVATE minemod::renderer minemod_doctest_main minemod_sprite_shaders)
minemod_configure_vulkan_target(minemod_renderer_resource_tests)

add_executable(minemod_renderer_resource_device_tests
    descriptors/descriptor_pools_test.cpp
    frame/frames_in_flight_test.cpp
    pipelines/pipeline_cache_test.cpp
    presentation/presenter_test.cpp
    textures/sampler_cache_test.cpp
    textures/texture_upload_test.cpp
    upload/upload_arena_test.cpp)
target_link_libraries(minemod_renderer_resource_device_tests
    PRIVATE minemod_renderer_test_support minemod_doctest_main minemod_sprite_shaders)
minemod_configure_vulkan_target(minemod_renderer_resource_device_tests)

add_executable(minemod_world_geometry_tests
    environment/fog_test.cpp
    math/matrix_test.cpp
    terrain/section_mesh_test.cpp
    terrain/section_selection_test.cpp
    terrain/section_space_test.cpp
    terrain/terrain_layer_test.cpp
    visibility/frustum_test.cpp)
target_link_libraries(minemod_world_geometry_tests PRIVATE minemod_renderer_test_support minemod_doctest_main)
minemod_configure_vulkan_target(minemod_world_geometry_tests)

add_executable(minemod_world_geometry_device_tests
    terrain/terrain_scene_test.cpp
    terrain/terrain_sections_test.cpp)
target_link_libraries(minemod_world_geometry_device_tests PRIVATE minemod_renderer_test_support minemod_doctest_main)
minemod_configure_vulkan_target(minemod_world_geometry_device_tests)

add_executable(minemod_scene_drawing_tests
    draw/material_test.cpp
    draw/primitive_mode_test.cpp
    draw/vertex_format_test.cpp
    pipelines/blending_test.cpp
    shaders/scene_shader_contract_test.cpp)
target_link_libraries(minemod_scene_drawing_tests
    PRIVATE minemod_renderer_test_support minemod_doctest_main minemod_scene_shaders)
minemod_configure_vulkan_target(minemod_scene_drawing_tests)

add_executable(minemod_scene_drawing_device_tests
    draw/batch_geometry_test.cpp
    draw/batch_scene_test.cpp
    scene/scene_composition_test.cpp
    scene/scene_frames_test.cpp)
target_link_libraries(minemod_scene_drawing_device_tests PRIVATE minemod_renderer_test_support minemod_doctest_main)
minemod_configure_vulkan_target(minemod_scene_drawing_device_tests)

add_library(minemod_scene_session_test_support STATIC
    session/support/running_session.cpp
    session/support/scene_shapes.cpp
    session/support/scene_stream.cpp)
target_link_libraries(minemod_scene_session_test_support
    PUBLIC minemod::scene_session minemod_abi_test_support minemod_renderer_test_support)
minemod_configure_vulkan_target(minemod_scene_session_test_support)

add_executable(minemod_scene_session_device_tests
    session/scene_corpus_test.cpp
    session/scene_frames_test.cpp
    session/scene_presentation_test.cpp
    session/scene_resources_test.cpp)
target_compile_definitions(minemod_scene_session_device_tests PRIVATE
    MINEMOD_COMMAND_STREAM_CORPUS="${PROJECT_SOURCE_DIR}/tests/fixtures/command-streams")
target_link_libraries(minemod_scene_session_device_tests
    PRIVATE minemod_scene_session_test_support minemod_doctest_main)
minemod_configure_vulkan_target(minemod_scene_session_device_tests)

add_test(NAME render_graph COMMAND minemod_render_graph_tests)
add_test(NAME render_graph_device COMMAND minemod_render_graph_device_tests)
add_test(NAME renderer_harness COMMAND minemod_renderer_tests)
add_test(NAME renderer_harness_cli COMMAND minemod_harness --validation --frames 2)
add_test(NAME renderer_resources COMMAND minemod_renderer_resource_tests)
add_test(NAME renderer_resources_device COMMAND minemod_renderer_resource_device_tests)
add_test(NAME scene_drawing COMMAND minemod_scene_drawing_tests)
add_test(NAME scene_drawing_device COMMAND minemod_scene_drawing_device_tests)
add_test(NAME scene_session_device COMMAND minemod_scene_session_device_tests)
add_test(NAME world_geometry COMMAND minemod_world_geometry_tests)
add_test(NAME world_geometry_device COMMAND minemod_world_geometry_device_tests)

set(minemod_device_tests
    render_graph_device
    renderer_harness
    renderer_harness_cli
    renderer_resources_device
    scene_drawing_device
    scene_session_device
    world_geometry_device)
set_tests_properties(${minemod_device_tests} PROPERTIES LABELS vulkan-device)

if(MINEMOD_ENABLE_SANITIZERS)
    set_tests_properties(${minemod_device_tests} PROPERTIES ENVIRONMENT "ASAN_OPTIONS=detect_leaks=0")
endif()
