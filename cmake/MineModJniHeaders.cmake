set(MINEMOD_JAVA_HOME "$ENV{JAVA_HOME}" CACHE PATH "JDK that provides the JNI headers")

if(WIN32)
    set(minemod_jni_platform win32)
else()
    set(minemod_jni_platform linux)
endif()

set(minemod_jni_include "${MINEMOD_JAVA_HOME}/include")
if(NOT EXISTS "${minemod_jni_include}/jni.h"
    OR NOT EXISTS "${minemod_jni_include}/${minemod_jni_platform}/jni_md.h")
    message(FATAL_ERROR
        "No JNI headers under MINEMOD_JAVA_HOME='${MINEMOD_JAVA_HOME}'. "
        "Point MINEMOD_JAVA_HOME at a JDK or turn MINEMOD_BUILD_JNI_BRIDGE off.")
endif()

add_library(minemod_jni_headers INTERFACE)
target_include_directories(minemod_jni_headers SYSTEM INTERFACE
    "${minemod_jni_include}"
    "${minemod_jni_include}/${minemod_jni_platform}")
