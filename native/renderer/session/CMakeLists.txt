add_library(minemod_scene_session STATIC
    src/abi/scene_session_abi.cpp
    src/commands/batch_option_commands.cpp
    src/commands/scene_commands.cpp
    src/commands/section_face_commands.cpp
    src/frame/frame_slot.cpp
    src/host/scene_host.cpp
    src/resources/texture_registry.cpp
    src/session/scene_session.cpp
    src/terrain/reachable_sections.cpp
    src/terrain/section_intake.cpp)
add_library(minemod::scene_session ALIAS minemod_scene_session)
target_include_directories(minemod_scene_session PUBLIC include PRIVATE src)
target_link_libraries(minemod_scene_session
    PUBLIC minemod::abi
    PRIVATE minemod::renderer)
minemod_configure_vulkan_target(minemod_scene_session)
