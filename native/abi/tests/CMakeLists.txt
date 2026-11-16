add_library(minemod_abi_test_support STATIC
    support/command_stream_corpus.cpp
    support/stream_builder.cpp
    support/test_session.cpp)
target_include_directories(minemod_abi_test_support PUBLIC "${CMAKE_CURRENT_SOURCE_DIR}")
target_link_libraries(minemod_abi_test_support PUBLIC minemod::abi)
minemod_configure_target(minemod_abi_test_support)

add_executable(minemod_abi_tests
    command_stream_corpus_test.cpp
    command_stream_validation_test.cpp
    negotiation_test.cpp
    session_test.cpp
    stream_mutation_test.cpp
    support/doctest_main.cpp)
target_compile_definitions(minemod_abi_tests PRIVATE
    MINEMOD_COMMAND_STREAM_CORPUS="${PROJECT_SOURCE_DIR}/tests/fixtures/command-streams")
target_link_libraries(minemod_abi_tests PRIVATE minemod_abi_test_support minemod_doctest)
minemod_configure_target(minemod_abi_tests)
add_test(NAME abi COMMAND minemod_abi_tests)

add_executable(minemod_abi_allocation_tests
    allocation_failure_test.cpp
    support/doctest_main.cpp)
target_link_libraries(minemod_abi_allocation_tests PRIVATE minemod_abi_test_support minemod_doctest)
minemod_configure_target(minemod_abi_allocation_tests)
add_test(NAME abi_allocation COMMAND minemod_abi_allocation_tests)

add_executable(minemod_abi_c_consumer c_consumer_test.c)
target_link_libraries(minemod_abi_c_consumer PRIVATE minemod::abi)
set_target_properties(minemod_abi_c_consumer PROPERTIES LINKER_LANGUAGE CXX)
minemod_configure_target(minemod_abi_c_consumer)
add_test(NAME abi_c_consumer COMMAND minemod_abi_c_consumer)
