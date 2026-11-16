find_package(Vulkan REQUIRED)
if(NOT Vulkan_GLSLANG_VALIDATOR_EXECUTABLE)
    message(FATAL_ERROR "glslangValidator is required to compile the renderer shaders")
endif()

function(minemod_configure_vulkan_target target)
    minemod_configure_target(${target})
    if(NOT MSVC)
        # Vulkan structures name only the fields they set; the language zeroes the rest.
        target_compile_options(${target} PRIVATE -Wno-missing-field-initializers)
    endif()
endfunction()

add_library(minemod_renderer STATIC
    src/device/device.cpp
    src/device/instance.cpp
    src/device/physical_device.cpp
    src/device/vulkan_error.cpp
    src/frame/frame_readback.cpp
    src/frame/frame_targets.cpp
    src/pipelines/shader_module.cpp
    src/presentation/headless_surface.cpp
    src/presentation/swapchain.cpp
    src/resources/buffer.cpp
    src/resources/image.cpp
    src/resources/image_layout.cpp
    src/resources/memory_type.cpp)
add_library(minemod::renderer ALIAS minemod_renderer)

target_include_directories(minemod_renderer PUBLIC src)
target_link_libraries(minemod_renderer PUBLIC Vulkan::Vulkan)
minemod_configure_vulkan_target(minemod_renderer)

add_subdirectory(harness)

if(MINEMOD_BUILD_TESTS)
    add_subdirectory(tests)
endif()
