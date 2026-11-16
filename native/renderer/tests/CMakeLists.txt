add_library(minemod_renderer_test_support STATIC
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

add_test(NAME render_graph COMMAND minemod_render_graph_tests)
add_test(NAME renderer_harness COMMAND minemod_renderer_tests)
add_test(NAME renderer_harness_cli COMMAND minemod_harness --validation --frames 2)
set_tests_properties(renderer_harness renderer_harness_cli PROPERTIES LABELS vulkan-device)

if(MINEMOD_ENABLE_SANITIZERS)
    set_tests_properties(renderer_harness renderer_harness_cli PROPERTIES ENVIRONMENT "ASAN_OPTIONS=detect_leaks=0")
endif()
