add_library(minemod_abi STATIC
    src/negotiation/negotiation.cpp
    src/resources/buffer_registry.cpp
    src/result/result_name.cpp
    src/session/session.cpp
    src/session/session_abi.cpp
    src/stream/command_stream_abi.cpp
    src/stream/command_stream_walker.cpp)
add_library(minemod::abi ALIAS minemod_abi)

target_include_directories(minemod_abi PUBLIC include PRIVATE src)
minemod_configure_target(minemod_abi)

if(MINEMOD_BUILD_TESTS)
    add_subdirectory(tests)
endif()
