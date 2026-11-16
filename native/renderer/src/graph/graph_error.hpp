#pragma once

#include <stdexcept>

namespace minemod::renderer {

class GraphError : public std::logic_error {
public:
    using std::logic_error::logic_error;
};

}
