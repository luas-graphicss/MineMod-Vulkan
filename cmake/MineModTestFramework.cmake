include(FetchContent)

FetchContent_Declare(doctest
    URL https://github.com/doctest/doctest/archive/refs/tags/v2.4.12.tar.gz
    URL_HASH SHA256=73381c7aa4dee704bd935609668cf41880ea7f19fa0504a200e13b74999c2d70
    SOURCE_SUBDIR headers-only)
FetchContent_MakeAvailable(doctest)

add_library(minemod_doctest INTERFACE)
target_include_directories(minemod_doctest SYSTEM INTERFACE "${doctest_SOURCE_DIR}/doctest")
