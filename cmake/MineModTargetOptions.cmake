function(minemod_configure_target target)
    target_compile_features(${target} PRIVATE cxx_std_20 c_std_17)
    set_target_properties(${target} PROPERTIES
        CXX_EXTENSIONS OFF
        C_EXTENSIONS OFF
        CXX_VISIBILITY_PRESET hidden
        C_VISIBILITY_PRESET hidden
        VISIBILITY_INLINES_HIDDEN ON
        POSITION_INDEPENDENT_CODE ON)

    if(MSVC)
        target_compile_options(${target} PRIVATE /W4 /permissive- /utf-8)
        if(MINEMOD_WARNINGS_AS_ERRORS)
            target_compile_options(${target} PRIVATE /WX)
        endif()
    else()
        target_compile_options(${target} PRIVATE -Wall -Wextra -Wpedantic -Wconversion -Wshadow)
        if(MINEMOD_WARNINGS_AS_ERRORS)
            target_compile_options(${target} PRIVATE -Werror)
        endif()
    endif()

    if(MINEMOD_ENABLE_SANITIZERS)
        if(MSVC)
            message(FATAL_ERROR "MINEMOD_ENABLE_SANITIZERS is only wired for GCC and Clang")
        endif()
        target_compile_options(${target} PRIVATE
            -fsanitize=address,undefined -fno-sanitize-recover=all -fno-omit-frame-pointer)
        target_link_options(${target} PRIVATE -fsanitize=address,undefined)
    endif()
endfunction()
