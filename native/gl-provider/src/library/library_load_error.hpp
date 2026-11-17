#pragma once

#include <stdexcept>

namespace minemod::provider {

class LibraryLoadError : public std::runtime_error {
public:
    using std::runtime_error::runtime_error;
};

}
