#pragma once

#include <jawt.h>

namespace minemod::jni {

using awt_entry_point = jboolean(JNICALL*)(JNIEnv*, JAWT*);

awt_entry_point find_awt_entry_point() noexcept;

}
