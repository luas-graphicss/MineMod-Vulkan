#include "library/process_environment.hpp"

#include <cstdlib>

namespace minemod::provider {

bool set_process_variable(const std::string& name, const std::string& value) {
    return ::setenv(name.c_str(), value.c_str(), 1) == 0;
}

}
