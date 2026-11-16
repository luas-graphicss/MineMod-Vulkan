add_library(minemod_scene_session STATIC
    src/abi/scene_session_abi.cpp
    src/abi/scene_sharing_abi.cpp
    src/commands/batch_option_commands.cpp
    src/commands/scene_commands.cpp
    src/commands/section_face_commands.cpp
    src/frame/frame_slot.cpp
    src/host/scene_host.cpp
    src/metrics/session_metrics.cpp
    src/reload/program_reload.cpp
    src/resources/texture_registry.cpp
    src/session/scene_session.cpp
    src/sharing/session_sharing.cpp
    src/targets/session_targets.cpp
    src/terrain/reachable_sections.cpp
    src/terrain/section_intake.cpp)
add_library(minemod::scene_session ALIAS minemod_scene_session)
target_include_directories(minemod_scene_session PUBLIC include PRIVATE src)
target_link_libraries(minemod_scene_session
    PUBLIC minemod::abi
    PRIVATE minemod::renderer)
if(CMAKE_SYSTEM_NAME STREQUAL "Linux")
    target_sources(minemod_scene_session PRIVATE
        src/sharing/interop_provider_sharing.cpp
        src/sharing/interop_shared_targets.cpp)
    target_link_libraries(minemod_scene_session PRIVATE minemod::interop)
else()
    target_sources(minemod_scene_session PRIVATE src/sharing/no_provider_sharing.cpp)
endif()
minemod_configure_vulkan_target(minemod_scene_session)
