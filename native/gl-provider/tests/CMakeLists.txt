set(minemod_fake_provider_directory "${CMAKE_CURRENT_BINARY_DIR}/provider")

add_library(minemod_fake_dependency SHARED fake_provider/fake_dependency.cpp)
add_library(minemod_fake_provider SHARED fake_provider/fake_egl.cpp fake_provider/fake_gl.cpp)
target_link_libraries(minemod_fake_provider PRIVATE minemod_fake_dependency)

foreach(fake_library minemod_fake_dependency minemod_fake_provider)
    minemod_configure_target(${fake_library})
    target_include_directories(${fake_library} PRIVATE "${CMAKE_CURRENT_SOURCE_DIR}")
    set_target_properties(${fake_library} PROPERTIES
        LIBRARY_OUTPUT_DIRECTORY "$<1:${minemod_fake_provider_directory}>"
        RUNTIME_OUTPUT_DIRECTORY "$<1:${minemod_fake_provider_directory}>"
        SKIP_BUILD_RPATH ON)
endforeach()
set_target_properties(minemod_fake_dependency PROPERTIES OUTPUT_NAME fake_dependency)
set_target_properties(minemod_fake_provider PROPERTIES OUTPUT_NAME fake_provider)

add_executable(minemod_gl_provider_tests
    calls/call_table_test.cpp
    calls/thunk_test.cpp
    context/provider_display_test.cpp
    library/driver_settings_test.cpp
    library/loaded_image_test.cpp
    library/provider_library_test.cpp
    session/provider_session_test.cpp
    support/fake_provider_library.cpp)
target_include_directories(minemod_gl_provider_tests PRIVATE "${CMAKE_CURRENT_SOURCE_DIR}")
target_compile_definitions(minemod_gl_provider_tests
    PRIVATE
        MINEMOD_FAKE_PROVIDER_DIRECTORY="${minemod_fake_provider_directory}"
        MINEMOD_FAKE_PROVIDER_FILE="$<TARGET_FILE_NAME:minemod_fake_provider>"
        MINEMOD_FAKE_DEPENDENCY_FILE="$<TARGET_FILE_NAME:minemod_fake_dependency>")
if(MSVC)
    target_compile_definitions(minemod_gl_provider_tests PRIVATE _CRT_SECURE_NO_WARNINGS)
endif()
target_link_libraries(minemod_gl_provider_tests PRIVATE minemod::gl_provider minemod_doctest_main)
add_dependencies(minemod_gl_provider_tests minemod_fake_provider)
minemod_configure_target(minemod_gl_provider_tests)

add_test(NAME gl_provider COMMAND minemod_gl_provider_tests)

set(MINEMOD_PROVIDER_BUNDLE "" CACHE PATH "Directory of a staged provider bundle that the bundle tests load")
if(MINEMOD_PROVIDER_BUNDLE)
    cmake_path(SET minemod_staged_bundle_directory NORMALIZE "${MINEMOD_PROVIDER_BUNDLE}")

    add_executable(minemod_gl_provider_bundle_tests
        bundle/staged_bundle_test.cpp
        support/staged_bundle.cpp)
    target_include_directories(minemod_gl_provider_bundle_tests PRIVATE "${CMAKE_CURRENT_SOURCE_DIR}")
    target_compile_definitions(minemod_gl_provider_bundle_tests
        PRIVATE MINEMOD_PROVIDER_BUNDLE_DIRECTORY="${minemod_staged_bundle_directory}")
    target_link_libraries(minemod_gl_provider_bundle_tests PRIVATE minemod::gl_provider minemod_doctest_main)
    minemod_configure_target(minemod_gl_provider_bundle_tests)

    add_test(NAME gl_provider_bundle COMMAND minemod_gl_provider_bundle_tests)
    set_tests_properties(gl_provider_bundle PROPERTIES LABELS vulkan-device)
endif()
