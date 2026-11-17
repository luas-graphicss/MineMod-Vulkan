add_library(minemod_interop_test_support STATIC
    support/sharing_device.cpp
    support/${minemod_interop_platform}/memory_import.cpp
    support/${minemod_interop_platform}/open_handles.cpp)
target_include_directories(minemod_interop_test_support PUBLIC "${CMAKE_CURRENT_SOURCE_DIR}")
target_link_libraries(minemod_interop_test_support PUBLIC minemod::interop)
if(WIN32)
    target_compile_definitions(minemod_interop_test_support PRIVATE NOMINMAX WIN32_LEAN_AND_MEAN)
endif()
minemod_configure_vulkan_target(minemod_interop_test_support)

add_executable(minemod_interop_tests
    handoff/ownership_transfer_test.cpp
    memory/exported_handle_test.cpp)
target_link_libraries(minemod_interop_tests PRIVATE minemod_interop_test_support minemod_doctest_main)
minemod_configure_vulkan_target(minemod_interop_tests)

add_executable(minemod_interop_device_tests
    contexts/device_identity_test.cpp
    handoff/shared_resource_handoff_test.cpp
    memory/shared_buffer_test.cpp
    memory/shared_image_test.cpp
    sync/shared_semaphore_test.cpp)
target_link_libraries(minemod_interop_device_tests
    PRIVATE
        minemod::interop_harness
        minemod_interop_test_support
        minemod_renderer_test_support
        minemod_doctest_main)
minemod_configure_vulkan_target(minemod_interop_device_tests)

add_test(NAME interop COMMAND minemod_interop_tests)
add_test(NAME interop_device COMMAND minemod_interop_device_tests)
set_tests_properties(interop_device PROPERTIES LABELS vulkan-device)

if(MINEMOD_ENABLE_SANITIZERS)
    set_tests_properties(interop_device PROPERTIES ENVIRONMENT "ASAN_OPTIONS=detect_leaks=0")
endif()
