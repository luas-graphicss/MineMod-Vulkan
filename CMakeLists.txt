cmake_minimum_required(VERSION 3.22)

project(MineModVulkan VERSION 0.1.0 LANGUAGES C CXX)

option(MINEMOD_BUILD_TESTS "Build the native test suites" ON)
option(MINEMOD_BUILD_JNI_BRIDGE "Build the JNI library that exposes the ABI to JVM clients" OFF)
option(MINEMOD_ENABLE_SANITIZERS "Instrument native targets with AddressSanitizer and UndefinedBehaviorSanitizer" OFF)
option(MINEMOD_WARNINGS_AS_ERRORS "Fail the build on compiler warnings" ON)

list(APPEND CMAKE_MODULE_PATH "${CMAKE_CURRENT_SOURCE_DIR}/cmake")
include(MineModTargetOptions)

if(MINEMOD_BUILD_TESTS)
    enable_testing()
    include(MineModTestFramework)
endif()

add_subdirectory(native/abi)

if(MINEMOD_BUILD_JNI_BRIDGE)
    include(MineModJniHeaders)
    add_subdirectory(native/jni-bridge)
endif()
