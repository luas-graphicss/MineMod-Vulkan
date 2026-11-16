set(minemod_fake_provider_directory "${CMAKE_CURRENT_BINARY_DIR}/provider")

add_library(minemod_fake_dependency SHARED fake_provider/fake_dependency.cpp)
add_library(minemod_fake_provider SHARED fake_provider/fake_egl.cpp fake_provider/fake_gl.cpp)
target_include_directories(minemod_fake_provider PRIVATE "${CMAKE_CURRENT_SOURCE_DIR}")
target_link_libraries(minemod_fake_provider PRIVATE minemod_fake_dependency ${CMAKE_DL_LIBS})

foreach(fake_library minemod_fake_dependency minemod_fake_provider)
    minemod_configure_target(${fake_library})
    set_target_properties(${fake_library} PROPERTIES
        LIBRARY_OUTPUT_DIRECTORY "${minemod_fake_provider_directory}"
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
    PRIVATE MINEMOD_FAKE_PROVIDER_DIRECTORY="${minemod_fake_provider_directory}")
target_link_libraries(minemod_gl_provider_tests PRIVATE minemod::gl_provider minemod_doctest_main)
add_dependencies(minemod_gl_provider_tests minemod_fake_provider)
minemod_configure_target(minemod_gl_provider_tests)

add_test(NAME gl_provider COMMAND minemod_gl_provider_tests)
